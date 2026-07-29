package com.csports.session;

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
import com.csports.session.dto.CancelTrainingSessionRequest;
import com.csports.session.dto.TrainingSessionResponse;
import com.csports.session.dto.UpdateTrainingSessionRequest;
import com.csports.session.exception.SessionStateConflictException;
import com.csports.sport.Sport;
import com.csports.sport.SportRepository;
import com.csports.trainer.TrainerProfile;
import com.csports.trainer.TrainerProfileRepository;
import com.csports.user.Role;
import com.csports.user.User;
import com.csports.user.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class TrainingSessionLifecycleTest {

    @Autowired
    private TrainingSessionService trainingSessionService;

    @Autowired
    private TrainingSessionRepository trainingSessionRepository;

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
    private TrainingSessionLifecycleJob lifecycleJob;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateAndCancellationNotifyBookedUsersAndPreserveSession() {
        String unique = UUID.randomUUID().toString();
        User trainer = saveUser("Trainer", "trainer-" + unique, Role.TRAINER);
        User participant = saveUser("Participant", "participant-" + unique, Role.USER);
        Sport sport = sportRepository.saveAndFlush(Sport.builder()
                .name("Lifecycle Sport " + unique)
                .build());
        trainerProfileRepository.saveAndFlush(TrainerProfile.builder()
                .user(trainer)
                .sport(sport)
                .bio("Lifecycle test trainer")
                .experienceYears(5)
                .build());

        LocalDate originalDate = LocalDate.now().plusDays(7);
        TrainingSession session = trainingSessionRepository.saveAndFlush(TrainingSession.builder()
                .trainer(trainer)
                .sport(sport)
                .title("Original session")
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
                .session(session)
                .build());

        authenticate(trainer);
        LocalDate updatedDate = originalDate.plusDays(1);
        DayOfWeek updatedDay = updatedDate.getDayOfWeek();
        trainingSessionService.updateSession(session.getId(), new UpdateTrainingSessionRequest(
                "The original venue is unavailable",
                "Updated session",
                "Updated details",
                "New Cairo",
                30.0285,
                31.4913,
                updatedDate,
                updatedDate,
                LocalTime.of(19, 0),
                90,
                Set.of(updatedDay),
                12,
                100.0));

        TrainingSession updated = trainingSessionRepository.findById(session.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Updated session");
        assertThat(updated.getPrice()).isEqualTo(100.0);
        assertThat(updated.getStatus()).isEqualTo(TrainingSessionStatus.SCHEDULED);
        assertThat(notificationsFor(participant))
                .extracting(UserNotification::getType)
                .containsExactly(NotificationType.SESSION_UPDATED);
        assertThat(notificationsFor(participant).getFirst().getMessage())
                .contains("The original venue is unavailable");

        trainingSessionService.cancelSession(
                session.getId(),
                new CancelTrainingSessionRequest("The trainer is unavailable"));

        TrainingSession cancelled = trainingSessionRepository.findById(session.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(TrainingSessionStatus.CANCELLED);
        assertThat(cancelled.getCancelledAt()).isNotNull();
        assertThat(bookingRepository.findByUserAndSessionAndStatus(
                participant,
                cancelled,
                BookingStatus.CONFIRMED)).isEmpty();
        assertThat(bookingRepository.findByUserAndSessionAndStatus(
                participant,
                cancelled,
                BookingStatus.CANCELLED_BY_TRAINER)).isPresent();
        assertThat(notificationsFor(participant))
                .extracting(UserNotification::getType)
                .containsExactlyInAnyOrder(
                        NotificationType.SESSION_UPDATED,
                        NotificationType.SESSION_CANCELLED);

        trainingSessionService.restoreSession(session.getId());

        TrainingSession restored = trainingSessionRepository.findById(session.getId()).orElseThrow();
        assertThat(restored.getStatus()).isEqualTo(TrainingSessionStatus.SCHEDULED);
        assertThat(restored.getCurrentParticipants()).isZero();
        assertThat(restored.getCancelledAt()).isNull();
        assertThat(bookingRepository.findByUserAndSessionAndStatus(
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
        LocalDate sessionDate = LocalDate.now().plusDays(7);
        TrainingSession session = saveSession(
                trainer,
                sport,
                "Original title",
                sessionDate,
                sessionDate,
                Set.of(sessionDate.getDayOfWeek()),
                0,
                5);
        long sessionCountBeforeUpdate = trainingSessionRepository.count();

        authenticate(trainer);
        TrainingSessionResponse response = trainingSessionService.updateSession(
                session.getId(),
                new UpdateTrainingSessionRequest(
                        "The original venue is unavailable",
                        null,
                        null,
                        "New Cairo",
                        30.0285,
                        31.4913,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        TrainingSession updated = trainingSessionRepository.findById(session.getId()).orElseThrow();
        assertThat(trainingSessionRepository.count()).isEqualTo(sessionCountBeforeUpdate);
        assertThat(response.id()).isEqualTo(session.getId());
        assertThat(updated.getLocationName()).isEqualTo("New Cairo");
        assertThat(updated.getLatitude()).isEqualTo(30.0285);
        assertThat(updated.getLongitude()).isEqualTo(31.4913);
        assertThat(updated.getTitle()).isEqualTo("Original title");
        assertThat(updated.getStartDate()).isEqualTo(sessionDate);
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
        LocalDate sessionDate = LocalDate.now().plusDays(7);
        TrainingSession session = saveSession(
                trainer,
                sport,
                "No-op session",
                sessionDate,
                sessionDate,
                Set.of(sessionDate.getDayOfWeek()),
                0,
                5);

        authenticate(trainer);
        assertThatThrownBy(() -> trainingSessionService.updateSession(
                session.getId(),
                new UpdateTrainingSessionRequest(
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
                        null)))
                .isInstanceOf(com.csports.session.exception.InvalidSessionScheduleException.class)
                .hasMessageContaining("At least one editable");
    }

    @Test
    void updateIsRejectedInsideTheTwoDayCutoff() {
        String unique = UUID.randomUUID().toString();
        User trainer = saveUser("Cutoff Trainer", "cutoff-" + unique, Role.TRAINER);
        Sport sport = sportRepository.saveAndFlush(Sport.builder()
                .name("Cutoff Sport " + unique)
                .build());
        LocalDate sessionDate = LocalDate.now().plusDays(1);
        TrainingSession session = saveSession(
                trainer,
                sport,
                "Too close to update",
                sessionDate,
                sessionDate,
                Set.of(sessionDate.getDayOfWeek()),
                0,
                10);

        authenticate(trainer);
        assertThatThrownBy(() -> trainingSessionService.updateSession(
                session.getId(),
                new UpdateTrainingSessionRequest(
                        "Minor correction",
                        "Still too close",
                        "Details",
                        "Nasr City",
                        30.0581,
                        31.3302,
                        sessionDate,
                        sessionDate,
                        LocalTime.of(18, 0),
                        60,
                        Set.of(sessionDate.getDayOfWeek()),
                        10,
                        100.0)))
                .isInstanceOf(SessionStateConflictException.class)
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
        TrainingSession session = saveSession(
                trainer,
                sport,
                "Future session",
                originalDate,
                originalDate,
                Set.of(originalDate.getDayOfWeek()),
                0,
                10);
        LocalDate requestedDate = LocalDate.now().plusDays(1);

        authenticate(trainer);
        assertThatThrownBy(() -> trainingSessionService.updateSession(
                session.getId(),
                new UpdateTrainingSessionRequest(
                        "Move the session sooner",
                        "Moved session",
                        "Details",
                        "Nasr City",
                        30.0581,
                        31.3302,
                        requestedDate,
                        requestedDate,
                        LocalTime.of(18, 0),
                        60,
                        Set.of(requestedDate.getDayOfWeek()),
                        10,
                        100.0)))
                .isInstanceOf(SessionStateConflictException.class)
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
        TrainingSession session = saveSession(
                trainer,
                sport,
                "Same day session",
                today,
                today.plusDays(1),
                Set.of(today.getDayOfWeek()),
                1,
                10);
        bookingRepository.saveAndFlush(Booking.builder()
                .user(participant)
                .session(session)
                .status(BookingStatus.CONFIRMED)
                .build());

        authenticate(trainer);
        assertThatThrownBy(() -> trainingSessionService.cancelSession(
                session.getId(),
                new CancelTrainingSessionRequest("Unexpected conflict")))
                .isInstanceOf(SessionStateConflictException.class)
                .hasMessageContaining("cannot be cancelled on a training day");

        assertThat(trainingSessionRepository.findById(session.getId()).orElseThrow().getStatus())
                .isEqualTo(TrainingSessionStatus.SCHEDULED);
    }

    @Test
    void sessionWithoutBookingsCanBeCancelledAfterItsStartDate() {
        String unique = UUID.randomUUID().toString();
        User trainer = saveUser("Late Cancel Trainer", "late-cancel-" + unique, Role.TRAINER);
        Sport sport = sportRepository.saveAndFlush(Sport.builder()
                .name("Late Cancel Sport " + unique)
                .build());
        LocalDate today = LocalDate.now();
        TrainingSession session = saveSession(
                trainer,
                sport,
                "Already started series",
                today.minusDays(1),
                today.plusDays(2),
                Set.of(today.plusDays(1).getDayOfWeek()),
                0,
                10);

        authenticate(trainer);
        trainingSessionService.cancelSession(
                session.getId(),
                new CancelTrainingSessionRequest("No participants and no longer needed"));

        assertThat(trainingSessionRepository.findById(session.getId()).orElseThrow().getStatus())
                .isEqualTo(TrainingSessionStatus.CANCELLED);
    }

    @Test
    void sessionCompletesOnlyAfterItsFinalOccurrenceEnds() {
        String unique = UUID.randomUUID().toString();
        User trainer = saveUser("Completion Trainer", "completion-" + unique, Role.TRAINER);
        Sport sport = sportRepository.saveAndFlush(Sport.builder()
                .name("Completion Sport " + unique)
                .build());
        LocalDate yesterday = LocalDate.now().minusDays(1);
        TrainingSession session = saveSession(
                trainer,
                sport,
                "Finished series",
                yesterday,
                yesterday,
                Set.of(yesterday.getDayOfWeek()),
                10,
                10);

        assertThat(session.getStatus()).isEqualTo(TrainingSessionStatus.SCHEDULED);
        lifecycleJob.completeEndedSessions();

        TrainingSession completed =
                trainingSessionRepository.findById(session.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(TrainingSessionStatus.COMPLETED);
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

    private TrainingSession saveSession(
            User trainer,
            Sport sport,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            Set<DayOfWeek> days,
            int currentParticipants,
            int maxParticipants) {
        return trainingSessionRepository.saveAndFlush(TrainingSession.builder()
                .trainer(trainer)
                .sport(sport)
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

    private java.util.List<UserNotification> notificationsFor(User user) {
        return notificationRepository
                .findByRecipient(user, PageRequest.of(0, 10))
                .getContent();
    }
}
