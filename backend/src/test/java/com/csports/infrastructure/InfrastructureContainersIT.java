package com.csports.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
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
import com.csports.booking.exception.CourseFullException;
import com.csports.infrastructure.redis.CacheNames;
import com.csports.infrastructure.redis.RedisKeys;
import com.csports.location.Region;
import com.csports.location.RegionRepository;
import com.csports.course.TrainingCourse;
import com.csports.course.TrainingCourseRepository;
import com.csports.course.TrainingCourseService;
import com.csports.course.dto.CourseSearchRequest;
import com.csports.course.dto.TrainingCourseResponse;
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
                            .asCompatibleSubstituteFor("postgres"))
                    // A different server default proves that the application's pool setting takes effect.
                    .withCommand("postgres", "-c", "default_transaction_isolation=repeatable read");

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7.4"))
                    .withExposedPorts(6379);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

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
    private TrainingCourseRepository trainingCourseRepository;

    @Autowired
    private TrainingCourseService trainingCourseService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SportRepository sportRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private com.csports.announcement.CourseAnnouncementService announcementService;

    @Autowired
    private com.csports.announcement.CourseAnnouncementRepository announcementRepository;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Test
    void publicationWaitsForConcurrentCancellationAndThenRejectsTheClosedCourse() throws Exception {
        String unique = UUID.randomUUID().toString();
        User trainer = saveUser("Notice Trainer", unique, Role.TRAINER);
        Sport sport = sportRepository.saveAndFlush(Sport.builder().name("Notice " + unique).build());
        Region region = regionRepository.saveAndFlush(Region.builder().name("Notice " + unique)
                .city("Cairo").country("Egypt").latitude(30.05).longitude(31.25).build());
        TrainingCourse course = saveSearchCourse(trainer, sport, region, "Concurrent notice", 30.05, 31.25);
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var publishing = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var cancelling = executor.submit(() -> new org.springframework.transaction.support.TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        var managed = trainingCourseRepository.findForAnnouncement(course.getId()).orElseThrow();
                        managed.setStatus(com.csports.course.TrainingCourseStatus.CANCELLED);
                        locked.countDown();
                        try {
                            if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Test release timed out");
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt(); throw new IllegalStateException(exception);
                        }
                    }));
            assertThat(locked.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var posting = executor.submit(() -> {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(trainer, null, trainer.getAuthorities()));
                publishing.countDown();
                try {
                    announcementService.publish(course.getId(), new com.csports.announcement.PublishAnnouncementRequest("Notice", "Too late"));
                    return "PUBLISHED";
                } catch (com.csports.course.exception.CourseStateConflictException expected) {
                    return "CLOSED";
                } finally { SecurityContextHolder.clearContext(); }
            });
            assertThat(publishing.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> posting.get(200, java.util.concurrent.TimeUnit.MILLISECONDS))
                    .isInstanceOf(java.util.concurrent.TimeoutException.class);
            release.countDown();
            cancelling.get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(posting.get(10, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo("CLOSED");
            assertThat(announcementRepository.findResponsesByCourseId(course.getId(), org.springframework.data.domain.PageRequest.of(0,10))).isEmpty();
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    @Test
    void applicationConnectionsUseReadCommittedDespiteTheServerDefault() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getTransactionIsolation()).isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
        }
    }

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
    void courseSearchCacheRoundTripsThroughRealRedis() {
        Cache cache = cacheManager.getCache(CacheNames.COURSE_SEARCH);
        assertThat(cache).isNotNull();
        PageResponse<TrainingCourseResponse> expected = new PageResponse<>(
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
    void postgisNearbySearchReturnsCoursesInDistanceOrder() {
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

        TrainingCourse nearest = saveSearchCourse(
                trainer, sport, region, "Nearby Search " + unique + " A", 30.051, 31.251);
        TrainingCourse farther = saveSearchCourse(
                trainer, sport, region, "Nearby Search " + unique + " B", 30.10, 31.30);

        PageResponse<TrainingCourseResponse> result = trainingCourseService.searchCourses(
                new CourseSearchRequest(
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

        assertThat(result.content()).extracting(TrainingCourseResponse::id)
                .containsExactly(nearest.getId(), farther.getId());
        assertThat(result.content()).allSatisfy(
                course -> assertThat(course.distanceMeters()).isNotNull().isPositive());
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
        LocalDate courseDate = LocalDate.now().plusDays(14);
        TrainingCourse course = trainingCourseRepository.saveAndFlush(
                TrainingCourse.builder()
                        .trainer(trainer)
                        .sport(sport)
                        .region(region)
                        .title("One remaining seat")
                        .description("Redis lock integration test")
                        .locationName("Cairo Club")
                        .latitude(30.05)
                        .longitude(31.25)
                        .startDate(courseDate)
                        .endDate(courseDate)
                        .startTime(LocalTime.of(18, 0))
                        .durationMinutes(60)
                        .days(Set.of(courseDate.getDayOfWeek()))
                        .maxParticipants(1)
                        .currentParticipants(0)
                        .price(300.0)
                        .build());

        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        List<String> outcomes = new ArrayList<>();
        try {
            var first = executor.submit(
                    () -> concurrentBookingOutcome(firstUser, course.getId(), start));
            var second = executor.submit(
                    () -> concurrentBookingOutcome(secondUser, course.getId(), start));
            start.countDown();
            outcomes.add(first.get());
            outcomes.add(second.get());
        } finally {
            executor.shutdownNow();
        }

        assertThat(outcomes).containsExactlyInAnyOrder("BOOKED", "FULL");

        TrainingCourse persisted =
                trainingCourseRepository.findById(course.getId()).orElseThrow();
        List<Booking> confirmed =
                bookingRepository.findAllByCourseAndStatus(
                        persisted,
                        BookingStatus.CONFIRMED);
        assertThat(persisted.getCurrentParticipants()).isEqualTo(1);
        assertThat(confirmed).hasSize(1);
        assertThat(stringRedisTemplate.hasKey(
                RedisKeys.BOOKING_LOCK_PREFIX + "{" + course.getId() + "}"))
                .isFalse();

        User bookedUser = confirmed.getFirst().getUser();
        assertThatThrownBy(() -> bookingRepository.saveAndFlush(Booking.builder()
                .user(bookedUser)
                .course(persisted)
                .status(BookingStatus.CONFIRMED)
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private String concurrentBookingOutcome(
            User user,
            Long courseId,
            CountDownLatch start) throws Exception {
        start.await();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()));
        try {
            bookingService.bookCourse(courseId);
            return "BOOKED";
        } catch (CourseFullException exception) {
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

    private TrainingCourse saveSearchCourse(
            User trainer,
            Sport sport,
            Region region,
            String title,
            double latitude,
            double longitude) {
        LocalDate courseDate = LocalDate.now().plusDays(20);
        return trainingCourseRepository.saveAndFlush(TrainingCourse.builder()
                .trainer(trainer)
                .sport(sport)
                .region(region)
                .title(title)
                .description("PostGIS nearby integration test")
                .locationName(region.getName())
                .latitude(latitude)
                .longitude(longitude)
                .startDate(courseDate)
                .endDate(courseDate.plusDays(30))
                .startTime(LocalTime.of(18, 0))
                .durationMinutes(60)
                .days(Set.of(courseDate.getDayOfWeek()))
                .maxParticipants(10)
                .currentParticipants(0)
                .price(300.0)
                .build());
    }
}
