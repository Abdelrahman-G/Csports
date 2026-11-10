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
import com.csports.course.TrainingCourse;
import com.csports.course.TrainingCourseRepository;
import com.csports.course.TrainingCourseStatus;
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
    private TrainingCourseRepository courseRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    private User user;
    private TrainingCourse course;
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
        LocalDate courseDate = LocalDate.now().plusDays(10);
        course = courseRepository.saveAndFlush(TrainingCourse.builder()
                .trainer(trainer)
                .sport(sport)
                .region(region)
                .title("Booking lifecycle course")
                .description("Tests booking history")
                .locationName("Cairo Club")
                .latitude(30.05)
                .longitude(31.25)
                .startDate(courseDate)
                .endDate(courseDate)
                .startTime(LocalTime.of(18, 0))
                .durationMinutes(60)
                .days(Set.of(courseDate.getDayOfWeek()))
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
                "/api/v1/bookings/" + course.getId());

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body())
                .contains("\"bookingStatus\":\"CONFIRMED\"")
                .contains("\"courseId\":" + course.getId())
                .contains("\"bookingOpen\":true")
                .contains("\"googleMapsUrl\":")
                .contains("\"remainingSeats\":1");

        HttpResponse<String> duplicate = request(
                "POST",
                "/api/v1/bookings/" + course.getId());
        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(duplicate.body()).contains("\"code\":\"RESOURCE_CONFLICT\"");

        HttpResponse<String> upcoming = request(
                "GET",
                "/api/v1/bookings/me");
        assertThat(upcoming.statusCode()).isEqualTo(200);
        assertThat(upcoming.body())
                .contains("\"bookingStatus\":\"CONFIRMED\"")
                .contains("\"totalElements\":1");

        HttpResponse<String> bookingForCourse = request(
                "GET",
                "/api/v1/bookings/me?view=ALL&status=CONFIRMED&courseId="
                        + course.getId());
        assertThat(bookingForCourse.statusCode()).isEqualTo(200);
        assertThat(bookingForCourse.body())
                .contains("\"courseId\":" + course.getId())
                .contains("\"totalElements\":1");

        HttpResponse<String> bookingForAnotherCourse = request(
                "GET",
                "/api/v1/bookings/me?view=ALL&status=CONFIRMED&courseId=999999");
        assertThat(bookingForAnotherCourse.statusCode()).isEqualTo(200);
        assertThat(bookingForAnotherCourse.body()).contains("\"totalElements\":0");

        HttpResponse<String> cancelled = request(
                "DELETE",
                "/api/v1/bookings/" + course.getId());
        assertThat(cancelled.statusCode()).isEqualTo(200);
        assertThat(cancelled.body())
                .contains("\"bookingStatus\":\"CANCELLED_BY_USER\"")
                .contains("\"googleMapsUrl\":null")
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
                .contains("\"googleMapsUrl\":null")
                .contains("\"totalElements\":1");

        HttpResponse<String> rebooked = request(
                "POST",
                "/api/v1/bookings/" + course.getId());
        assertThat(rebooked.statusCode()).isEqualTo(201);

        TrainingCourse updatedCourse =
                courseRepository.findById(course.getId()).orElseThrow();
        assertThat(updatedCourse.getCurrentParticipants()).isEqualTo(1);
        assertThat(bookingRepository.findAllByCourseAndStatus(
                updatedCourse,
                BookingStatus.CONFIRMED)).hasSize(1);
        assertThat(bookingRepository.findAllByCourseAndStatus(
                updatedCourse,
                BookingStatus.CANCELLED_BY_USER)).hasSize(1);
    }

    @Test
    void completedCourseBookingCannotChangeHistoricalParticipantCount()
            throws Exception {
        assertThat(request("POST", "/api/v1/bookings/" + course.getId()).statusCode())
                .isEqualTo(201);

        TrainingCourse completed =
                courseRepository.findById(course.getId()).orElseThrow();
        completed.setStatus(TrainingCourseStatus.COMPLETED);
        courseRepository.saveAndFlush(completed);

        HttpResponse<String> response = request(
                "DELETE",
                "/api/v1/bookings/" + course.getId());

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains(
                "\"code\":\"RESOURCE_CONFLICT\"",
                "cannot be cancelled after the course series is complete");
        assertThat(courseRepository.findById(course.getId())
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
