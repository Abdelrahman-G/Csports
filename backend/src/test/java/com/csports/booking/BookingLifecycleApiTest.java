package com.csports.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.csports.location.Region;
import com.csports.location.RegionRepository;
import com.csports.security.JwtService;
import com.csports.session.TrainingSession;
import com.csports.session.TrainingSessionRepository;
import com.csports.session.TrainingSessionStatus;
import com.csports.sport.Sport;
import com.csports.sport.SportRepository;
import com.csports.user.Role;
import com.csports.user.User;
import com.csports.user.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BookingLifecycleApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SportRepository sportRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private TrainingSessionRepository sessionRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    private User user;
    private TrainingSession session;
    private String accessToken;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString();
        User trainer = saveUser("Booking Trainer", "trainer-" + unique, Role.TRAINER);
        user = saveUser("Booking User", "user-" + unique, Role.USER);
        Sport sport = sportRepository.saveAndFlush(Sport.builder()
                .name("Booking Sport " + unique)
                .build());
        Region region = regionRepository.saveAndFlush(Region.builder()
                .name("Booking Region " + unique)
                .city("Cairo")
                .country("Egypt")
                .latitude(30.05)
                .longitude(31.25)
                .build());
        LocalDate sessionDate = LocalDate.now().plusDays(10);
        session = sessionRepository.saveAndFlush(TrainingSession.builder()
                .trainer(trainer)
                .sport(sport)
                .region(region)
                .title("Booking lifecycle session")
                .description("Tests booking history")
                .locationName("Cairo Club")
                .latitude(30.05)
                .longitude(31.25)
                .startDate(sessionDate)
                .endDate(sessionDate)
                .startTime(LocalTime.of(18, 0))
                .durationMinutes(60)
                .days(Set.of(sessionDate.getDayOfWeek()))
                .maxParticipants(2)
                .currentParticipants(0)
                .price(250.0)
                .build());
        accessToken = jwtService.generateAccessToken(user);
    }

    @Test
    void bookingCancellationHistoryAndRebookingAreExposedToTheFrontend()
            throws Exception {
        HttpResponse<String> created = request(
                "POST",
                "/api/v1/bookings/" + session.getId());

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body())
                .contains("\"bookingStatus\":\"CONFIRMED\"")
                .contains("\"sessionId\":" + session.getId())
                .contains("\"bookingOpen\":true")
                .contains("\"googleMapsUrl\":")
                .contains("\"remainingSeats\":1");

        HttpResponse<String> duplicate = request(
                "POST",
                "/api/v1/bookings/" + session.getId());
        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(duplicate.body()).contains("\"code\":\"RESOURCE_CONFLICT\"");

        HttpResponse<String> upcoming = request(
                "GET",
                "/api/v1/bookings/me");
        assertThat(upcoming.statusCode()).isEqualTo(200);
        assertThat(upcoming.body())
                .contains("\"bookingStatus\":\"CONFIRMED\"")
                .contains("\"totalElements\":1");

        HttpResponse<String> cancelled = request(
                "DELETE",
                "/api/v1/bookings/" + session.getId());
        assertThat(cancelled.statusCode()).isEqualTo(200);
        assertThat(cancelled.body())
                .contains("\"bookingStatus\":\"CANCELLED_BY_USER\"")
                .containsPattern("\"cancelledAt\":\"[^\"]+\"");

        HttpResponse<String> emptyUpcoming = request(
                "GET",
                "/api/v1/bookings/me");
        assertThat(emptyUpcoming.body()).contains("\"totalElements\":0");

        HttpResponse<String> history = request(
                "GET",
                "/api/v1/bookings/me?view=HISTORY&status=CANCELLED_BY_USER");
        assertThat(history.statusCode()).isEqualTo(200);
        assertThat(history.body())
                .contains("\"bookingStatus\":\"CANCELLED_BY_USER\"")
                .contains("\"totalElements\":1");

        HttpResponse<String> rebooked = request(
                "POST",
                "/api/v1/bookings/" + session.getId());
        assertThat(rebooked.statusCode()).isEqualTo(201);

        TrainingSession updatedSession =
                sessionRepository.findById(session.getId()).orElseThrow();
        assertThat(updatedSession.getCurrentParticipants()).isEqualTo(1);
        assertThat(bookingRepository.findAllBySessionAndStatus(
                updatedSession,
                BookingStatus.CONFIRMED)).hasSize(1);
        assertThat(bookingRepository.findAllBySessionAndStatus(
                updatedSession,
                BookingStatus.CANCELLED_BY_USER)).hasSize(1);
    }

    @Test
    void completedSessionBookingCannotChangeHistoricalParticipantCount()
            throws Exception {
        assertThat(request("POST", "/api/v1/bookings/" + session.getId()).statusCode())
                .isEqualTo(201);

        TrainingSession completed =
                sessionRepository.findById(session.getId()).orElseThrow();
        completed.setStatus(TrainingSessionStatus.COMPLETED);
        sessionRepository.saveAndFlush(completed);

        HttpResponse<String> response = request(
                "DELETE",
                "/api/v1/bookings/" + session.getId());

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains(
                "\"code\":\"RESOURCE_CONFLICT\"",
                "cannot be cancelled after the session series is complete");
        assertThat(sessionRepository.findById(session.getId())
                .orElseThrow()
                .getCurrentParticipants()).isEqualTo(1);
    }

    private HttpResponse<String> request(String method, String path) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + accessToken);
        if ("POST".equals(method)) {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        } else if ("DELETE".equals(method)) {
            builder.DELETE();
        } else {
            builder.GET();
        }
        return HttpClient.newHttpClient().send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private User saveUser(String name, String unique, Role role) {
        return userRepository.saveAndFlush(User.builder()
                .name(name)
                .email(unique + "@csports.test")
                .phoneNumber("+201" + Math.abs(unique.hashCode()) + "55")
                .password("not-used")
                .age(30)
                .role(role)
                .build());
    }
}
