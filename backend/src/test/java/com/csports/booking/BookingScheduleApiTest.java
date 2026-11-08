package com.csports.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class BookingScheduleApiTest {
    @LocalServerPort private int port;
    @Autowired private UserRepository users;
    @Autowired private SportRepository sports;
    @Autowired private RegionRepository regions;
    @Autowired private TrainingSessionRepository courses;
    @Autowired private BookingRepository bookings;
    @Autowired private JwtService jwt;
    @MockitoBean private RedisTemplate<String, Object> redisTemplate;

    private User trainee;
    private User trainer;
    private TrainingSession course;

    @BeforeEach
    void setUp() {
        trainee = user(Role.USER);
        trainer = user(Role.TRAINER);
        var sport = sports.saveAndFlush(Sport.builder().name("Schedule " + UUID.randomUUID()).build());
        var region = regions.saveAndFlush(Region.builder().name("Schedule " + UUID.randomUUID())
                .city("Cairo").country("Egypt").latitude(30.05).longitude(31.25).build());
        course = createCourse(sport, region);
    }

    private TrainingSession createCourse(Sport sport, Region region) {
        return courses.saveAndFlush(TrainingSession.builder().trainer(trainer).sport(sport).region(region)
                .title("Schedule test course").locationName("Test venue").latitude(30.05).longitude(31.25)
                .startDate(LocalDate.of(2026, 11, 1)).endDate(LocalDate.of(2026, 11, 30))
                .startTime(LocalTime.of(18, 0)).durationMinutes(90)
                .days(Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
                .maxParticipants(20).price(100.0).build());
    }

    @Test
    void cancellationsKeepStartedOccurrencesAndRemoveThoseStartingAtTheCutoff() throws Exception {
        booking(trainee, LocalDateTime.of(2026, 11, 9, 18, 30));
        var response = get(trainee, "from=2026-11-07&to=2026-11-14");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"locationName\":\"Test venue\"")
                .doesNotContain("\"trainerName\"", "\"regionName\"");
        assertThat(response.body()).contains("2026-11-09T18:00:00", "Africa/Cairo").doesNotContain("2026-11-12T18:00:00");
        var booking = bookings.findAllBySessionAndStatus(course, BookingStatus.CANCELLED_BY_USER).getFirst();
        booking.setCancelledAt(LocalDateTime.of(2026, 11, 9, 18, 0));
        bookings.saveAndFlush(booking);
        assertThat(get(trainee, "from=2026-11-07&to=2026-11-14").body()).contains("\"entries\":[]");
    }

    @Test
    void overnightTrainingIsReturnedOnceWhenItContinuesIntoTheRequestedDay() throws Exception {
        course.setDays(Set.of(DayOfWeek.FRIDAY));
        course.setStartTime(LocalTime.of(23, 0));
        course = courses.saveAndFlush(course);
        booking(trainee, null);

        var response = get(trainee, "from=2026-11-07&to=2026-11-08");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("2026-11-06T23:00:00+02:00", "2026-11-07T00:30:00+02:00");
        assertThat(response.body().split("\"bookingId\":", -1)).hasSize(2);
    }

    @Test
    void trainerCancellationAndCompletedCoursesPreserveEarlierTraining() throws Exception {
        booking(trainee, null);
        course.setStatus(TrainingSessionStatus.CANCELLED);
        course.setCancelledAt(LocalDateTime.of(2026, 11, 10, 12, 0));
        course = courses.saveAndFlush(course);
        assertThat(get(trainee, "from=2026-11-07&to=2026-11-14").body())
                .contains("2026-11-09T18:00:00").doesNotContain("2026-11-12T18:00:00");
        course.setStatus(TrainingSessionStatus.COMPLETED);
        course.setCancelledAt(null);
        course = courses.saveAndFlush(course);
        assertThat(get(trainee, "from=2026-11-07&to=2026-11-14").body()).contains("2026-11-12T18:00:00");
    }

    @Test
    void validatesRangeAndEnforcesAuthenticationRoleAndOwnership() throws Exception {
        booking(trainee, null);
        assertThat(get(trainee, "from=2026-11-14&to=2026-11-07").statusCode()).isEqualTo(400);
        assertThat(get(trainee, "from=2026-11-01&to=2026-12-03").statusCode()).isEqualTo(400);
        assertThat(get(trainee, "from=2026-11-07").statusCode()).isEqualTo(400);
        assertThat(get(trainee, "from=bad&to=2026-11-14").statusCode()).isEqualTo(400);
        assertThat(get(null, "from=2026-11-07&to=2026-11-14").statusCode()).isEqualTo(401);
        assertThat(get(trainer, "from=2026-11-07&to=2026-11-14").statusCode()).isEqualTo(403);
        assertThat(get(user(Role.USER), "from=2026-11-07&to=2026-11-14").body()).contains("\"entries\":[]");
    }

    @Test
    void cancelledLegacyBookingWithoutATimestampDoesNotInventTrainingDates() throws Exception {
        var booking = booking(trainee, null);
        booking.setStatus(BookingStatus.CANCELLED_BY_USER);
        bookings.saveAndFlush(booking);
        assertThat(get(trainee, "from=2026-11-07&to=2026-11-14").body()).contains("\"entries\":[]");
    }

    @Test
    void includesAllBookingsRatherThanOnePageAndExcludesPreEnrollmentDates() throws Exception {
        for (int i = 0; i < 13; i++) {
            course = createCourse(course.getSport(), course.getRegion());
            booking(trainee, null);
        }
        var body = get(trainee, "from=2026-11-07&to=2026-11-14").body();
        assertThat(body.split("\"bookingId\":", -1)).hasSize(27);
        var late = user(Role.USER);
        var booking = booking(late, null);
        jdbc.update("update booking set booked_at = ? where id = ?", LocalDateTime.of(2026, 11, 10, 12, 0), booking.getId());
        assertThat(get(late, "from=2026-11-07&to=2026-11-14").body())
                .doesNotContain("2026-11-09T18:00:00").contains("2026-11-12T18:00:00");
    }

    private Booking booking(User user, LocalDateTime cancelledAt) {
        var booking = Booking.builder().user(user).session(course)
                .status(cancelledAt == null ? BookingStatus.CONFIRMED : BookingStatus.CANCELLED_BY_USER)
                .cancelledAt(cancelledAt).build();
        // Fixed fixture timestamps make historical-window tests independent of the wall clock.
        booking = bookings.saveAndFlush(booking);
        bookingsTimestamp(booking.getId());
        return booking;
    }

    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
    private void bookingsTimestamp(Long id) {
        jdbc.update("update booking set booked_at = ? where id = ?", LocalDateTime.of(2026, 10, 1, 12, 0), id);
    }

    private User user(Role role) {
        String unique = UUID.randomUUID().toString();
        return users.saveAndFlush(User.builder().name("Schedule " + role).email(unique + "@test.invalid")
                .phoneNumber(unique).password("unused").age(25).role(role).build());
    }

    private HttpResponse<String> get(User user, String query) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/bookings/me/schedule?" + query));
        if (user != null) request.header("Authorization", "Bearer " + jwt.generateAccessToken(user));
        return HttpClient.newHttpClient().send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
