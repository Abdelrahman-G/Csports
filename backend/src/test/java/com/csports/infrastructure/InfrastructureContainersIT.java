package com.csports.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.csports.common.pagination.PageResponse;
import com.csports.booking.Booking;
import com.csports.booking.BookingRepository;
import com.csports.booking.BookingService;
import com.csports.booking.BookingStatus;
import com.csports.booking.exception.SessionFullException;
import com.csports.infrastructure.redis.CacheNames;
import com.csports.infrastructure.redis.RedisKeys;
import com.csports.location.Region;
import com.csports.location.RegionRepository;
import com.csports.session.TrainingSession;
import com.csports.session.TrainingSessionRepository;
import com.csports.session.TrainingSessionService;
import com.csports.session.dto.SessionSearchRequest;
import com.csports.session.dto.TrainingSessionResponse;
import com.csports.sport.Sport;
import com.csports.sport.SportRepository;
import com.csports.user.Role;
import com.csports.user.User;
import com.csports.user.UserRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

@SpringBootTest
@ActiveProfiles("integration")
@Testcontainers(disabledWithoutDocker = true)
class InfrastructureContainersIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    DockerImageName.parse("postgis/postgis:17-3.5")
                            .asCompatibleSubstituteFor("postgres"));

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7.4"))
                    .withExposedPorts(6379);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TrainingSessionRepository trainingSessionRepository;

    @Autowired
    private TrainingSessionService trainingSessionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SportRepository sportRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Test
    void flywayAndRedisAreAvailableWithRealInfrastructure() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true",
                Integer.class
        );
        String redisResponse;
        try (var connection = redisConnectionFactory.getConnection()) {
            redisResponse = connection.ping();
        }

        assertThat(migrationCount).isPositive();
        assertThat(redisResponse).isEqualToIgnoringCase("PONG");
    }

    @Test
    void sessionSearchCacheRoundTripsThroughRealRedis() {
        Cache cache = cacheManager.getCache(CacheNames.SESSION_SEARCH);
        assertThat(cache).isNotNull();
        PageResponse<TrainingSessionResponse> expected = new PageResponse<>(
                List.of(),
                0,
                10,
                0,
                0,
                true,
                true);

        cache.put("integration-test", expected);
        PageResponse<?> actual = cache.get("integration-test", PageResponse.class);

        assertThat(actual).isEqualTo(expected);
        cache.evict("integration-test");
    }

    @Test
    void postgisNearbySearchReturnsSessionsInDistanceOrder() {
        String unique = UUID.randomUUID().toString();
        User trainer = saveUser("Nearby Trainer", "nearby-" + unique, Role.TRAINER);
        Sport sport = sportRepository.saveAndFlush(Sport.builder()
                .name("Nearby Sport " + unique)
                .build());
        Region region = regionRepository.saveAndFlush(Region.builder()
                .name("Nearby Region " + unique)
                .city("Cairo")
                .country("Egypt")
                .latitude(30.05)
                .longitude(31.25)
                .build());

        TrainingSession nearest = saveSearchSession(
                trainer, sport, region, "Nearby Search " + unique + " A", 30.051, 31.251);
        TrainingSession farther = saveSearchSession(
                trainer, sport, region, "Nearby Search " + unique + " B", 30.10, 31.30);

        PageResponse<TrainingSessionResponse> result = trainingSessionService.searchSessions(
                new SessionSearchRequest(
                        "Nearby Search " + unique,
                        null,
                        null,
                        null,
                        30.05,
                        31.25,
                        20.0,
                        null,
                        null,
                        null,
                        null,
                        true,
                        "distance",
                        "asc",
                        0,
                        10));

        assertThat(result.content()).extracting(TrainingSessionResponse::id)
                .containsExactly(nearest.getId(), farther.getId());
        assertThat(result.content()).allSatisfy(
                session -> assertThat(session.distanceMeters()).isNotNull().isPositive());
        assertThat(result.content().getFirst().distanceMeters())
                .isLessThan(result.content().getLast().distanceMeters());
    }

    @Test
    void redisLockSerializesLastSeatAndPostgresEnforcesActiveBookingUniqueness()
            throws Exception {
        String unique = UUID.randomUUID().toString();
        User trainer = saveUser("Concurrent Trainer", "trainer-" + unique, Role.TRAINER);
        User firstUser = saveUser("Concurrent User One", "one-" + unique, Role.USER);
        User secondUser = saveUser("Concurrent User Two", "two-" + unique, Role.USER);
        Sport sport = sportRepository.saveAndFlush(Sport.builder()
                .name("Concurrent Sport " + unique)
                .build());
        Region region = regionRepository.saveAndFlush(Region.builder()
                .name("Concurrent Region " + unique)
                .city("Cairo")
                .country("Egypt")
                .latitude(30.05)
                .longitude(31.25)
                .build());
        LocalDate sessionDate = LocalDate.now().plusDays(14);
        TrainingSession session = trainingSessionRepository.saveAndFlush(
                TrainingSession.builder()
                        .trainer(trainer)
                        .sport(sport)
                        .region(region)
                        .title("One remaining seat")
                        .description("Redis lock integration test")
                        .locationName("Cairo Club")
                        .latitude(30.05)
                        .longitude(31.25)
                        .startDate(sessionDate)
                        .endDate(sessionDate)
                        .startTime(LocalTime.of(18, 0))
                        .durationMinutes(60)
                        .days(Set.of(sessionDate.getDayOfWeek()))
                        .maxParticipants(1)
                        .currentParticipants(0)
                        .price(300.0)
                        .build());

        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        List<String> outcomes = new ArrayList<>();
        try {
            var first = executor.submit(
                    () -> concurrentBookingOutcome(firstUser, session.getId(), start));
            var second = executor.submit(
                    () -> concurrentBookingOutcome(secondUser, session.getId(), start));
            start.countDown();
            outcomes.add(first.get());
            outcomes.add(second.get());
        } finally {
            executor.shutdownNow();
        }

        assertThat(outcomes).containsExactlyInAnyOrder("BOOKED", "FULL");

        TrainingSession persisted =
                trainingSessionRepository.findById(session.getId()).orElseThrow();
        List<Booking> confirmed =
                bookingRepository.findAllBySessionAndStatus(
                        persisted,
                        BookingStatus.CONFIRMED);
        assertThat(persisted.getCurrentParticipants()).isEqualTo(1);
        assertThat(confirmed).hasSize(1);
        assertThat(stringRedisTemplate.hasKey(
                RedisKeys.BOOKING_LOCK_PREFIX + "{" + session.getId() + "}"))
                .isFalse();

        User bookedUser = confirmed.getFirst().getUser();
        assertThatThrownBy(() -> bookingRepository.saveAndFlush(Booking.builder()
                .user(bookedUser)
                .session(persisted)
                .status(BookingStatus.CONFIRMED)
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private String concurrentBookingOutcome(
            User user,
            Long sessionId,
            CountDownLatch start) throws Exception {
        start.await();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()));
        try {
            bookingService.bookSession(sessionId);
            return "BOOKED";
        } catch (SessionFullException exception) {
            return "FULL";
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private User saveUser(String name, String unique, Role role) {
        return userRepository.saveAndFlush(User.builder()
                .name(name)
                .email(unique + "@csports.test")
                .phoneNumber("+201" + Math.abs(unique.hashCode()) + "77")
                .password("not-used")
                .age(30)
                .role(role)
                .build());
    }

    private TrainingSession saveSearchSession(
            User trainer,
            Sport sport,
            Region region,
            String title,
            double latitude,
            double longitude) {
        LocalDate sessionDate = LocalDate.now().plusDays(20);
        return trainingSessionRepository.saveAndFlush(TrainingSession.builder()
                .trainer(trainer)
                .sport(sport)
                .region(region)
                .title(title)
                .description("PostGIS nearby integration test")
                .locationName(region.getName())
                .latitude(latitude)
                .longitude(longitude)
                .startDate(sessionDate)
                .endDate(sessionDate.plusDays(30))
                .startTime(LocalTime.of(18, 0))
                .durationMinutes(60)
                .days(Set.of(sessionDate.getDayOfWeek()))
                .maxParticipants(10)
                .currentParticipants(0)
                .price(300.0)
                .build());
    }
}
