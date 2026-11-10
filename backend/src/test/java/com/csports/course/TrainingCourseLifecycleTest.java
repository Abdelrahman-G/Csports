package com.csports.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.csports.booking.Booking;
import com.csports.booking.BookingRepository;
import com.csports.booking.BookingStatus;
import com.csports.notification.NotificationType;
import com.csports.notification.UserNotification;
import com.csports.notification.UserNotificationRepository;
import com.csports.location.Region;
import com.csports.location.RegionRepository;
import com.csports.course.dto.CancelTrainingCourseRequest;
import com.csports.course.dto.TrainingCourseResponse;
import com.csports.course.dto.UpdateTrainingCourseRequest;
import com.csports.course.exception.CourseStateConflictException;
import com.csports.sport.Sport;
import com.csports.sport.SportRepository;
import com.csports.trainer.TrainerProfile;
import com.csports.trainer.TrainerProfileRepository;
import com.csports.user.Role;
import com.csports.user.User;
import com.csports.user.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class TrainingCourseLifecycleTest {

    @Autowired
    private TrainingCourseService trainingCourseService;

    @Autowired
    private TrainingCourseRepository trainingCourseRepository;

    @Autowired
    private TrainerProfileRepository trainerProfileRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserNotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SportRepository sportRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private TrainingCourseLifecycleJob lifecycleJob;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateAndCancellationNotifyBookedUsersAndPreserveCourse() {
        String unique = UUID.randomUUID().toString();
        User trainer = saveUser("Trainer", "trainer-" + unique, Role.TRAINER);
        User participant = saveUser("Participant", "participant-" + unique, Role.USER);
        Sport sport = sportRepository.saveAndFlush(Sport.builder()
                .name("Lifecycle Sport " + unique)
                .build());
        Region region = saveRegion("Lifecycle Region " + unique);
        trainerProfileRepository.saveAndFlush(TrainerProfile.builder()
                .user(trainer)
                .sport(sport)
                .bio("Lifecycle test trainer")
                .experienceYears(5)
                .build());

        LocalDate originalDate = LocalDate.now().plusDays(7);
        TrainingCourse course = trainingCourseRepository.saveAndFlush(TrainingCourse.builder()
                .trainer(trainer)
                .sport(sport)
                .region(region)
                .title("Original course")
                .description("Original details")
                .locationName("Nasr City")
                .latitude(30.0581)
                .longitude(31.3302)
                .startDate(originalDate)
                .endDate(originalDate)
                .startTime(LocalTime.of(18, 0))
                .durationMinutes(60)
                .days(Set.of(originalDate.getDayOfWeek()))
                .maxParticipants(10)
                .currentParticipants(1)
                .price(100.0)
                .build());
        bookingRepository.saveAndFlush(Booking.builder()
                .user(participant)
                .course(course)
                .build());

        authenticate(trainer);
        LocalDate updatedDate = originalDate.plusDays(1);
        DayOfWeek updatedDay = updatedDate.getDayOfWeek();
        trainingCourseService.updateCourse(course.getId(), new UpdateTrainingCourseRequest(
                "The original venue is unavailable",
                "Updated course",
                "Updated details",
                "New Cairo",
                region.getId(),
                30.0285,
                31.4913,
                updatedDate,
                updatedDate,
                LocalTime.of(19, 0),
                90,
                Set.of(updatedDay),
                12,
                100.0));

        TrainingCourse updated = trainingCourseRepository.findById(course.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Updated course");
        assertThat(updated.getPrice()).isEqualTo(100.0);
        assertThat(updated.getStatus()).isEqualTo(TrainingCourseStatus.SCHEDULED);
        assertThat(notificationsFor(participant))
                .extracting(UserNotification::getType)
                .containsExactly(NotificationType.COURSE_UPDATED);
        assertThat(notificationsFor(participant).getFirst().getMessage())
                .contains("The original venue is unavailable");

        trainingCourseService.cancelCourse(
                course.getId(),
                new CancelTrainingCourseRequest("The trainer is unavailable"));

        TrainingCourse cancelled = trainingCourseRepository.findById(course.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(TrainingCourseStatus.CANCELLED);
        assertThat(cancelled.getCancelledAt()).isNotNull();
        assertThat(bookingRepository.findByUserAndCourseAndStatus(
                participant,
                cancelled,
                BookingStatus.CONFIRMED)).isEmpty();
        assertThat(bookingRepository.findByUserAndCourseAndStatus(
                participant,
                cancelled,
                BookingStatus.CANCELLED_BY_TRAINER)).isPresent();
        assertThat(notificationsFor(participant))
                .extracting(UserNotification::getType)
                .containsExactlyInAnyOrder(
                        NotificationType.COURSE_UPDATED,
                        NotificationType.COURSE_CANCELLED);

        trainingCourseService.restoreCourse(course.getId());

        TrainingCourse restored = trainingCourseRepository.findById(course.getId()).orElseThrow();
        assertThat(restored.getStatus()).isEqualTo(TrainingCourseStatus.SCHEDULED);
        assertThat(restored.getCurrentParticipants()).isZero();
        assertThat(restored.getCancelledAt()).isNull();
        assertThat(bookingRepository.findByUserAndCourseAndStatus(
                participant,
                restored,
                BookingStatus.CANCELLED_BY_TRAINER)).isPresent();
    }

    @Test
    void partialUpdateChangesOnlySuppliedFields() {
        String unique = UUID.randomUUID().toString();
        User trainer = saveUser("Partial Trainer", "partial-" + unique, Role.TRAINER);
        Sport sport = sportRepository.saveAndFlush(Sport.builder()
                .name("Partial Sport " + unique)
                .build());
        LocalDate courseDate = LocalDate.now().plusDays(7);
        TrainingCourse course = saveCourse(
                trainer,
                sport,
                "Original title",
                courseDate,
                courseDate,
                Set.of(courseDate.getDayOfWeek()),
                0,
                5);
        long courseCountBeforeUpdate = trainingCourseRepository.count();

        authenticate(trainer);
        TrainingCourseResponse response = trainingCourseService.updateCourse(
                course.getId(),
                new UpdateTrainingCourseRequest(
                        "The original venue is unavailable",
                        null,
                        null,
                        "New Cairo",
                        null,
                        30.0285,
                        31.4913,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        TrainingCourse updated = trainingCourseRepository.findById(course.getId()).orElseThrow();
        assertThat(trainingCourseRepository.count()).isEqualTo(courseCountBeforeUpdate);
        assertThat(response.id()).isEqualTo(course.getId());
        assertThat(updated.getLocationName()).isEqualTo("New Cairo");
        assertThat(updated.getLatitude()).isEqualTo(30.0285);
        assertThat(updated.getLongitude()).isEqualTo(31.4913);
        assertThat(updated.getTitle()).isEqualTo("Original title");
        assertThat(updated.getStartDate()).isEqualTo(courseDate);
        assertThat(updated.getMaxParticipants()).isEqualTo(5);
        assertThat(updated.getPrice()).isEqualTo(100.0);
    }

    @Test
    void updateWithReasonButNoEditableFieldIsRejected() {
        String unique = UUID.randomUUID().toString();
        User trainer = saveUser("No-op Trainer", "no-op-" + unique, Role.TRAINER);
        Sport sport = sportRepository.saveAndFlush(Sport.builder()
                .name("No-op Sport " + unique)
                .build());
        LocalDate courseDate = LocalDate.now().plusDays(7);
        TrainingCourse course = saveCourse(
                trainer,
                sport,
                "No-op course",
                courseDate,
                courseDate,
                Set.of(courseDate.getDayOfWeek()),
                0,
                5);

        authenticate(trainer);
        assertThatThrownBy(() -> trainingCourseService.updateCourse(
                course.getId(),
                new UpdateTrainingCourseRequest(
                        "A reason without an actual change",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)))
                .isInstanceOf(com.csports.course.exception.InvalidCourseScheduleException.class)
                .hasMessageContaining("At least one editable");
    }

    @Test
    void updateIsRejectedInsideTheTwoDayCutoff() {
        String unique = UUID.randomUUID().toString();
        User trainer = saveUser("Cutoff Trainer", "cutoff-" + unique, Role.TRAINER);
        Sport sport = sportRepository.saveAndFlush(Sport.builder()
                .name("Cutoff Sport " + unique)
                .build());
        LocalDate courseDate = LocalDate.now().plusDays(1);
        TrainingCourse course = saveCourse(
                trainer,
                sport,
                "Too close to update",
                courseDate,
                courseDate,
                Set.of(courseDate.getDayOfWeek()),
                0,
                10);

        authenticate(trainer);
        assertThatThrownBy(() -> trainingCourseService.updateCourse(
                course.getId(),
                new UpdateTrainingCourseRequest(
                        "Minor correction",
                        "Still too close",
                        "Details",
                        "Nasr City",
                        null,
                        30.0581,
                        31.3302,
                        courseDate,
                        courseDate,
                        LocalTime.of(18, 0),
                        60,
                        Set.of(courseDate.getDayOfWeek()),
                        10,
                        100.0)))
                .isInstanceOf(CourseStateConflictException.class)
                .hasMessageContaining("more than two days");
    }

    @Test
    void updateCannotMoveTheFirstOccurrenceInsideTheTwoDayCutoff() {
        String unique = UUID.randomUUID().toString();
        User trainer = saveUser("Reschedule Trainer", "reschedule-" + unique, Role.TRAINER);
        Sport sport = sportRepository.saveAndFlush(Sport.builder()
                .name("Reschedule Sport " + unique)
                .build());
        LocalDate originalDate = LocalDate.now().plusDays(7);
        TrainingCourse course = saveCourse(
                trainer,
                sport,
                "Future course",
                originalDate,
                originalDate,
                Set.of(originalDate.getDayOfWeek()),
                0,
                10);
        LocalDate requestedDate = LocalDate.now().plusDays(1);

        authenticate(trainer);
        assertThatThrownBy(() -> trainingCourseService.updateCourse(
                course.getId(),
                new UpdateTrainingCourseRequest(
                        "Move the course sooner",
                        "Moved course",
                        "Details",
                        "Nasr City",
                        null,
                        30.0581,
                        31.3302,
                        requestedDate,
                        requestedDate,
                        LocalTime.of(18, 0),
                        60,
                        Set.of(requestedDate.getDayOfWeek()),
                        10,
                        100.0)))
                .isInstanceOf(CourseStateConflictException.class)
                .hasMessageContaining("updated first occurrence");
    }

    @Test
    void cancellationWithBookingsIsRejectedOnTrainingDay() {
        String unique = UUID.randomUUID().toString();
        User trainer = saveUser("Same Day Trainer", "same-day-trainer-" + unique, Role.TRAINER);
        User participant = saveUser("Same Day User", "same-day-user-" + unique, Role.USER);
        Sport sport = sportRepository.saveAndFlush(Sport.builder()
                .name("Same Day Sport " + unique)
                .build());
        LocalDate today = LocalDate.now();
        TrainingCourse course = saveCourse(
                trainer,
                sport,
                "Same day course",
                today,
                today.plusDays(1),
                Set.of(today.getDayOfWeek()),
                1,
                10);
        bookingRepository.saveAndFlush(Booking.builder()
                .user(participant)
                .course(course)
                .status(BookingStatus.CONFIRMED)
                .build());

        authenticate(trainer);
        assertThatThrownBy(() -> trainingCourseService.cancelCourse(
                course.getId(),
                new CancelTrainingCourseRequest("Unexpected conflict")))
                .isInstanceOf(CourseStateConflictException.class)
                .hasMessageContaining("cannot be cancelled on a training day");

        assertThat(trainingCourseRepository.findById(course.getId()).orElseThrow().getStatus())
                .isEqualTo(TrainingCourseStatus.SCHEDULED);
    }

    @Test
    void courseWithoutBookingsCanBeCancelledAfterItsStartDate() {
        String unique = UUID.randomUUID().toString();
        User trainer = saveUser("Late Cancel Trainer", "late-cancel-" + unique, Role.TRAINER);
        Sport sport = sportRepository.saveAndFlush(Sport.builder()
                .name("Late Cancel Sport " + unique)
                .build());
        LocalDate today = LocalDate.now();
        TrainingCourse course = saveCourse(
                trainer,
                sport,
                "Already started series",
                today.minusDays(1),
                today.plusDays(2),
                Set.of(today.plusDays(1).getDayOfWeek()),
                0,
                10);

        authenticate(trainer);
        trainingCourseService.cancelCourse(
                course.getId(),
                new CancelTrainingCourseRequest("No participants and no longer needed"));

        assertThat(trainingCourseRepository.findById(course.getId()).orElseThrow().getStatus())
                .isEqualTo(TrainingCourseStatus.CANCELLED);
    }

    @Test
    void courseCompletesOnlyAfterItsFinalOccurrenceEnds() {
        String unique = UUID.randomUUID().toString();
        User trainer = saveUser("Completion Trainer", "completion-" + unique, Role.TRAINER);
        Sport sport = sportRepository.saveAndFlush(Sport.builder()
                .name("Completion Sport " + unique)
                .build());
        LocalDate yesterday = LocalDate.now().minusDays(1);
        TrainingCourse course = saveCourse(
                trainer,
                sport,
                "Finished series",
                yesterday,
                yesterday,
                Set.of(yesterday.getDayOfWeek()),
                10,
                10);

        assertThat(course.getStatus()).isEqualTo(TrainingCourseStatus.SCHEDULED);
        lifecycleJob.completeEndedCourses();

        TrainingCourse completed =
                trainingCourseRepository.findById(course.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(TrainingCourseStatus.COMPLETED);
        assertThat(completed.getCurrentParticipants()).isEqualTo(completed.getMaxParticipants());
    }

    private User saveUser(String name, String unique, Role role) {
        return userRepository.saveAndFlush(User.builder()
                .name(name)
                .phoneNumber("+201" + Math.abs(unique.hashCode()) + "99")
                .email(unique + "@csports.test")
                .password("not-used")
                .age(30)
                .role(role)
                .build());
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private TrainingCourse saveCourse(
            User trainer,
            Sport sport,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            Set<DayOfWeek> days,
            int currentParticipants,
            int maxParticipants) {
        return trainingCourseRepository.saveAndFlush(TrainingCourse.builder()
                .trainer(trainer)
                .sport(sport)
                .region(saveRegion("Course Region " + java.util.UUID.randomUUID()))
                .title(title)
                .description("Lifecycle rules test")
                .locationName("Nasr City")
                .latitude(30.0581)
                .longitude(31.3302)
                .startDate(startDate)
                .endDate(endDate)
                .startTime(LocalTime.of(18, 0))
                .durationMinutes(60)
                .days(days)
                .maxParticipants(maxParticipants)
                .currentParticipants(currentParticipants)
                .price(100.0)
                .build());
    }

    private Region saveRegion(String name) {
        return regionRepository.saveAndFlush(Region.builder()
                .country("Egypt")
                .city("Cairo")
                .name(name)
                .latitude(30.0581)
                .longitude(31.3302)
                .build());
    }

    private java.util.List<UserNotification> notificationsFor(User user) {
        return notificationRepository
                .findByRecipient(user, PageRequest.of(0, 10))
                .getContent();
    }
}
