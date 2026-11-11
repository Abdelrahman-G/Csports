package com.csports.announcement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import com.csports.booking.*;
import com.csports.course.*;
import com.csports.location.*;
import com.csports.notification.*;
import com.csports.security.JwtService;
import com.csports.sport.*;
import com.csports.user.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CourseAnnouncementApiTest {
    @LocalServerPort int port;
    @Autowired UserRepository users;
    @Autowired SportRepository sports;
    @Autowired RegionRepository regions;
    @Autowired TrainingCourseRepository courses;
    @Autowired BookingRepository bookings;
    @Autowired CourseAnnouncementRepository announcements;
    @Autowired UserNotificationRepository notifications;
    @Autowired JwtService jwt;
    @Autowired com.csports.trainer.TrainerProfileRepository trainerProfiles;
    @MockitoSpyBean NotificationService notificationService;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;
    private User trainer, trainee, newcomer, otherTrainer;
    private TrainingCourse course;

    @BeforeEach
    void prepare() {
        trainer = user(Role.TRAINER); otherTrainer = user(Role.TRAINER);
        trainee = user(Role.USER); newcomer = user(Role.USER);
        var sport = sports.saveAndFlush(Sport.builder().name("Notice sport " + UUID.randomUUID()).build());
        trainerProfiles.saveAndFlush(com.csports.trainer.TrainerProfile.builder().user(trainer).sport(sport).build());
        var region = regions.saveAndFlush(Region.builder().name("Notice region " + UUID.randomUUID())
                .city("Cairo").country("Egypt").latitude(30.05).longitude(31.25).build());
        LocalDate date = LocalDate.now(CourseSchedule.TIME_ZONE).plusDays(14);
        course = courses.saveAndFlush(TrainingCourse.builder().trainer(trainer).sport(sport).region(region)
                .title("Noticeboard course").locationName("Club").latitude(30.05).longitude(31.25)
                .startDate(date).endDate(date.plusWeeks(4)).startTime(LocalTime.of(18,0))
                .durationMinutes(60).days(Set.of(date.getDayOfWeek())).maxParticipants(10).price(100.0).build());
    }

    @Test
    void newMembersReadHistoryWithoutReceivingOldAlertsAndCancellationRevokesAccess() throws Exception {
        Booking booking = enrol(trainee);
        HttpResponse<String> created = post(trainer, "{\"title\":\"Bring water\",\"content\":\"Bring a bottle.\"}");
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(notifications.countByRecipientAndReadFalse(trainee)).isEqualTo(1);
        assertThat(notifications.countByRecipientAndReadFalse(newcomer)).isZero();
        assertThat(request("GET", path(), newcomer, null).statusCode()).isEqualTo(403);
        enrol(newcomer);
        assertThat(request("GET", path(), newcomer, null).body()).contains("Bring water", "\"canPublish\":false");
        assertThat(notifications.countByRecipientAndReadFalse(newcomer)).isZero();
        booking.setStatus(BookingStatus.CANCELLED_BY_USER); booking.setCancelledAt(LocalDateTime.now());
        bookings.saveAndFlush(booking);
        assertThat(request("GET", path(), trainee, null).statusCode()).isEqualTo(403);
        post(trainer, "{\"title\":\"Follow-up\",\"content\":\"Bring a towel too.\"}");
        assertThat(notifications.countByRecipientAndReadFalse(trainee)).isEqualTo(1);
        assertThat(notifications.countByRecipientAndReadFalse(newcomer)).isEqualTo(1);
        enrol(trainee);
        assertThat(request("GET", path(), trainee, null).body()).contains("Bring water", "Follow-up");
    }

    @Test
    void onlyOwnerPublishesAndInputIsValidatedWithoutEditOrDeleteEndpoints() throws Exception {
        String valid = "{\"title\":\"Notice\",\"content\":\"Message\"}";
        assertThat(post(otherTrainer, valid).statusCode()).isEqualTo(403);
        assertThat(post(trainee, valid).statusCode()).isEqualTo(403);
        assertThat(request("GET", path(), null, null).statusCode()).isEqualTo(401);
        assertThat(post(trainer, "{\"title\":\"  \",\"content\":\"Message\"}").statusCode()).isEqualTo(400);
        assertThat(post(trainer, "{\"title\":\"" + "x".repeat(151) + "\",\"content\":\"Message\"}").statusCode()).isEqualTo(400);
        assertThat(post(trainer, "{\"title\":\"Notice\",\"content\":\"" + "x".repeat(2001) + "\"}").statusCode()).isEqualTo(400);
        assertThat(request("GET", path() + "?page=-1", trainer, null).statusCode()).isEqualTo(400);
        assertThat(request("GET", path() + "?size=51", trainer, null).statusCode()).isEqualTo(400);
        long id = id(post(trainer, valid).body());
        assertThat(request("PATCH", path() + "/" + id, trainer, valid).statusCode()).isEqualTo(405);
        assertThat(request("DELETE", path() + "/" + id, trainer, null).statusCode()).isEqualTo(405);
        assertThat(request("GET", path() + "/" + id, trainer, null).body()).contains("Message");
    }

    @Test
    void emptyAudienceStillGetsHistoryAndTrainerCannotForgeAnAutomaticNotice() throws Exception {
        var response = post(trainer, "{\"title\":\"Welcome\",\"content\":\"Bring water\",\"type\":\"COURSE_CANCELLED\"}");
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains("TRAINER_MESSAGE").doesNotContain("COURSE_CANCELLED");
        assertThat(request("GET", path(), trainer, null).body()).contains("Welcome", "\"totalElements\":1");
        assertThat(notifications.countByRecipientAndReadFalse(trainee)).isZero();
    }

    @Test
    void finishedAndCancelledCoursesAreReadOnlyEvenBeforeLifecycleJobRuns() throws Exception {
        enrol(trainee);
        post(trainer, "{\"title\":\"History\",\"content\":\"Keep this\"}");
        LocalDate past = LocalDate.now(CourseSchedule.TIME_ZONE).minusDays(2);
        course.setStartDate(past); course.setEndDate(past); course.setDays(Set.of(past.getDayOfWeek()));
        course = courses.saveAndFlush(course);
        assertThat(post(trainer, "{\"title\":\"Late\",\"content\":\"No\"}").statusCode()).isEqualTo(409);
        assertThat(request("GET", path(), trainer, null).body()).contains("\"canPublish\":false", "History");
        assertThat(request("GET", path(), trainee, null).statusCode()).isEqualTo(200);
        course = courses.findById(course.getId()).orElseThrow();
        course.setStatus(TrainingCourseStatus.CANCELLED); course = courses.saveAndFlush(course);
        assertThat(post(trainer, "{\"title\":\"Late\",\"content\":\"No\"}").statusCode()).isEqualTo(409);
    }

    @Test
    void automaticChangesNotifyOnceAndRestorationDoesNotRestoreMembership() throws Exception {
        enrol(trainee);
        String coursePath = "/api/v1/courses/" + course.getId();
        assertThat(request("PATCH", coursePath, trainer, "{\"reason\":\"Later start\",\"startTime\":\"19:00\"}").statusCode()).isEqualTo(200);
        assertThat(notifications.countByRecipientAndReadFalse(trainee)).isEqualTo(1);
        assertThat(request("GET", path(), trainee, null).body()).contains("Later start", "19:00");
        request("PATCH", coursePath, trainer, "{\"reason\":\"Same time\",\"startTime\":\"19:00\"}");
        assertThat(notifications.countByRecipientAndReadFalse(trainee)).isEqualTo(1);
        assertThat(request("PATCH", coursePath + "/cancel", trainer, "{\"reason\":\"Venue closed\"}").statusCode()).isEqualTo(200);
        assertThat(notifications.countByRecipientAndReadFalse(trainee)).isEqualTo(2);
        assertThat(request("GET", path(), trainee, null).statusCode()).isEqualTo(403);
        assertThat(request("GET", coursePath, trainee, null).body()).contains("Venue closed", "CANCELLED");
        assertThat(request("PATCH", coursePath + "/restore", trainer, null).statusCode()).isEqualTo(200);
        assertThat(request("GET", path(), trainer, null).body()).contains("Course reopened", "Venue closed", "\"totalElements\":3");
        assertThat(request("GET", path(), trainee, null).statusCode()).isEqualTo(403);
        assertThat(notifications.countByRecipientAndReadFalse(trainee)).isEqualTo(2);
    }

    @Test
    void oldLinksAreCourseScopedAndPagesAreNewestFirst() throws Exception {
        long first = id(post(trainer, "{\"title\":\"First\",\"content\":\"One\"}").body());
        long second = id(post(trainer, "{\"title\":\"Second\",\"content\":\"Two\"}").body());
        assertThat(request("GET", path() + "?size=1", trainer, null).body()).contains("Second").doesNotContain("First");
        assertThat(request("GET", path() + "/" + first, trainer, null).body()).contains("First");
        var alien = announcements.saveAndFlush(new CourseAnnouncement(courses.saveAndFlush(TrainingCourse.builder()
                .trainer(trainer).sport(course.getSport()).region(course.getRegion()).title("Another course")
                .locationName("Club").latitude(30.05).longitude(31.25).startDate(course.getStartDate()).endDate(course.getEndDate())
                .startTime(course.getStartTime()).days(course.getDays()).durationMinutes(60).maxParticipants(10).price(10.0).build()),
                trainer, AnnouncementType.TRAINER_MESSAGE, "Private", "Other course", Instant.now()));
        assertThat(request("GET", path() + "/" + alien.getId(), trainer, null).statusCode()).isEqualTo(404);
        assertThat(second).isGreaterThan(first);
    }

    @Test
    void failedAlertCreationRollsBackTheAnnouncement() throws Exception {
        enrol(trainee);
        NotificationService target = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(notificationService);
        doThrow(new IllegalStateException("Simulated notification failure")).when(target)
                .createCourseNotifications(eq(course.getId()), anyLong(), any(), anyString(), anyString(), anyList());
        assertThat(post(trainer, "{\"title\":\"Rollback\",\"content\":\"No partial write\"}").statusCode()).isEqualTo(500);
        assertThat(announcements.findResponsesByCourseId(course.getId(), PageRequest.of(0,10))).isEmpty();
        assertThat(notifications.countByRecipientAndReadFalse(trainee)).isZero();
    }

    private Booking enrol(User user) {
        return bookings.saveAndFlush(Booking.builder().course(course).user(user).build());
    }
    private User user(Role role) {
        String unique = UUID.randomUUID().toString();
        return users.saveAndFlush(User.builder().name(role.name()).email(unique + "@csports.test")
                .phoneNumber(unique).password("unused").age(30).role(role).build());
    }
    private String path() { return "/api/v1/courses/" + course.getId() + "/announcements"; }
    private HttpResponse<String> post(User actor, String body) throws Exception { return request("POST", path(), actor, body); }
    private long id(String json) {
        var match = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(json);
        assertThat(match.find()).isTrue(); return Long.parseLong(match.group(1));
    }
    private HttpResponse<String> request(String method, String path, User actor, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (actor != null) request.header("Authorization", "Bearer " + jwt.generateAccessToken(actor));
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
