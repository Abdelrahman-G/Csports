package com.csports.session;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csports.booking.Booking;
import com.csports.booking.BookingMapper;
import com.csports.booking.BookingRepository;
import com.csports.booking.BookingStatus;
import com.csports.common.exception.ForbiddenOperationException;
import com.csports.common.exception.ResourceNotFoundException;
import com.csports.common.pagination.PageResponse;
import com.csports.notification.NotificationType;
import com.csports.notification.SessionChangedEvent;
import com.csports.session.dto.CreateTrainingSessionRequest;
import com.csports.session.dto.CancelTrainingSessionRequest;
import com.csports.session.dto.SessionParticipantResponse;
import com.csports.session.dto.TrainingSessionDetailsResponse;
import com.csports.session.dto.TrainingSessionResponse;
import com.csports.session.dto.UpdateTrainingSessionRequest;
import com.csports.session.exception.InvalidSessionScheduleException;
import com.csports.session.exception.SessionStateConflictException;
import com.csports.session.exception.TrainingSessionNotFoundException;
import com.csports.sport.Sport;
import com.csports.trainer.TrainerProfile;
import com.csports.trainer.TrainerProfileRepository;
import com.csports.trainer.exception.TrainerProfileNotFoundException;
import com.csports.user.User;
import com.csports.user.UserService;

@Service
public class TrainingSessionService {

    private final TrainingSessionRepository trainingSessionRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final UserService userService;
    private final TrainingSessionMapper trainingSessionMapper;
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final ApplicationEventPublisher eventPublisher;

    public TrainingSessionService(
            TrainingSessionRepository trainingSessionRepository,
            TrainerProfileRepository trainerProfileRepository,
            UserService userService,
            TrainingSessionMapper trainingSessionMapper,
            BookingRepository bookingRepository,
            BookingMapper bookingMapper,
            ApplicationEventPublisher eventPublisher) {
        this.trainingSessionRepository = trainingSessionRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.userService = userService;
        this.trainingSessionMapper = trainingSessionMapper;
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TrainingSessionResponse createSession(CreateTrainingSessionRequest request) {
        validateSchedule(request.startDate(), request.endDate(), request.startTime(), request.days());

        User trainer = userService.getCurrentUser();
        TrainerProfile trainerProfile = trainerProfileRepository.findByUser(trainer)
                .orElseThrow(TrainerProfileNotFoundException::new);

        Sport sport = trainerProfile.getSport();
        TrainingSession session = TrainingSession.builder()
                .trainer(trainer)
                .sport(sport)
                .description(request.description())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .title(request.title())
                .locationName(request.locationName())
                .price(request.price())
                .maxParticipants(request.maxParticipants())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .startTime(request.startTime())
                .durationMinutes(request.durationMinutes())
                .days(Set.copyOf(request.days()))
                .status(TrainingSessionStatus.SCHEDULED)
                .build();
        TrainingSession savedSession = trainingSessionRepository.save(session);
        return trainingSessionMapper.toResponse(savedSession);
    }

    @Transactional(readOnly = true)
    public PageResponse<TrainingSessionResponse> getAllUpcomingSessions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").ascending());

        Page<TrainingSession> sessions =
                trainingSessionRepository.findByStatusAndStartDateGreaterThanEqual(
                        TrainingSessionStatus.SCHEDULED,
                        LocalDate.now(),
                        pageable);

        return toSessionPage(sessions);
    }

    @Transactional(readOnly = true)
    public TrainingSessionDetailsResponse getSession(Long sessionId) {
        TrainingSession session = trainingSessionRepository.findById(sessionId)
                .orElseThrow(TrainingSessionNotFoundException::new);

        TrainerProfile trainerProfile = trainerProfileRepository.findByUser(session.getTrainer())
                .orElseThrow(TrainerProfileNotFoundException::new);
        return trainingSessionMapper.toDetailsResponse(session, trainerProfile);
    }

    @Transactional(readOnly = true)
    public PageResponse<TrainingSessionResponse> getTrainerSessions(int page, int size) {
        User trainer = userService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").ascending());
        return toSessionPage(trainingSessionRepository.findByTrainer(trainer, pageable));
    }

    @Transactional
    public TrainingSessionResponse updateSession(
            Long sessionId,
            UpdateTrainingSessionRequest request) {
        User trainer = userService.getCurrentUser();
        TrainingSession session = getOwnedSession(sessionId, trainer);
        requireScheduled(session);

        if (request.price() != null && Double.compare(request.price(), session.getPrice()) != 0) {
            throw new SessionStateConflictException(
                    "Session price cannot be changed after the session is created.");
        }
        if (!request.hasEditableFields()) {
            throw new InvalidSessionScheduleException(
                    "At least one editable session field must be provided.");
        }

        String updatedTitle = request.title() != null
                ? request.title()
                : session.getTitle();
        String updatedDescription = request.description() != null
                ? request.description()
                : session.getDescription();
        String updatedLocationName = request.locationName() != null
                ? request.locationName()
                : session.getLocationName();
        Double updatedLatitude = request.latitude() != null
                ? request.latitude()
                : session.getLatitude();
        Double updatedLongitude = request.longitude() != null
                ? request.longitude()
                : session.getLongitude();
        LocalDate updatedStartDate = request.startDate() != null
                ? request.startDate()
                : session.getStartDate();
        LocalDate updatedEndDate = request.endDate() != null
                ? request.endDate()
                : session.getEndDate();
        LocalTime updatedStartTime = request.startTime() != null
                ? request.startTime()
                : session.getStartTime();
        Integer updatedDurationMinutes = request.durationMinutes() != null
                ? request.durationMinutes()
                : session.getDurationMinutes();
        Set<DayOfWeek> updatedDays = request.days() != null
                ? Set.copyOf(request.days())
                : Set.copyOf(session.getDays());
        Integer updatedMaxParticipants = request.maxParticipants() != null
                ? request.maxParticipants()
                : session.getMaxParticipants();

        validateSchedule(
                updatedStartDate,
                updatedEndDate,
                updatedStartTime,
                updatedDays);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime updateDeadline = SessionSchedule.firstStart(session).minusDays(2);
        LocalDateTime requestedScheduleDeadline = SessionSchedule.firstStart(
                updatedStartDate,
                updatedEndDate,
                updatedStartTime,
                updatedDays).minusDays(2);
        if (!now.isBefore(updateDeadline) || !now.isBefore(requestedScheduleDeadline)) {
            throw new SessionStateConflictException(
                    "A session can only be updated when more than two days remain before both its current and updated first occurrence.");
        }

        if (updatedMaxParticipants < session.getCurrentParticipants()) {
            throw new SessionStateConflictException(
                    "Maximum participants cannot be lower than the number of existing bookings.");
        }

        boolean locationChanged =
                !Objects.equals(session.getLocationName(), updatedLocationName)
                || !Objects.equals(session.getLatitude(), updatedLatitude)
                || !Objects.equals(session.getLongitude(), updatedLongitude);

        session.setTitle(updatedTitle);
        session.setDescription(updatedDescription);
        session.setLocationName(updatedLocationName);
        session.setLatitude(updatedLatitude);
        session.setLongitude(updatedLongitude);
        session.setStartDate(updatedStartDate);
        session.setEndDate(updatedEndDate);
        session.setStartTime(updatedStartTime);
        session.setDurationMinutes(updatedDurationMinutes);
        session.setDays(updatedDays);
        session.setMaxParticipants(updatedMaxParticipants);
        session.setLastUpdateReason(request.reason());

        if (locationChanged) {
            publishSessionChange(
                    session,
                    NotificationType.SESSION_UPDATED,
                    request.reason(),
                    bookingRepository.findBookedUserIdsBySessionId(session.getId()));
        }

        return trainingSessionMapper.toResponse(session);
    }

    @Transactional
    public void cancelSession(Long sessionId, CancelTrainingSessionRequest request) {
        User trainer = userService.getCurrentUser();
        TrainingSession session = getOwnedSession(sessionId, trainer);
        requireScheduled(session);

        List<Booking> activeBookings = bookingRepository.findAllBySessionAndStatus(
                session,
                BookingStatus.CONFIRMED);

        if (!activeBookings.isEmpty()) {
            LocalDate nextOccurrence = SessionSchedule.nextOccurrenceOnOrAfter(
                    session,
                    LocalDate.now());
            if (nextOccurrence == null) {
                throw new SessionStateConflictException(
                        "This session has no remaining occurrence to cancel.");
            }
            if (nextOccurrence.isEqual(LocalDate.now())) {
                throw new SessionStateConflictException(
                        "A session with active bookings cannot be cancelled on a training day.");
            }
        }

        session.setStatus(TrainingSessionStatus.CANCELLED);
        session.setCancelledAt(LocalDateTime.now());
        session.setCancellationReason(request.reason());
        session.setCurrentParticipants(0);

        List<Long> recipientIds = activeBookings.stream()
                .map(booking -> booking.getUser().getId())
                .distinct()
                .toList();
        activeBookings.forEach(booking ->
                booking.setStatus(BookingStatus.CANCELLED_BY_TRAINER));

        publishSessionChange(
                session,
                NotificationType.SESSION_CANCELLED,
                request.reason(),
                recipientIds);
    }

    @Transactional
    public void restoreSession(Long sessionId) {
        User trainer = userService.getCurrentUser();
        TrainingSession session = getOwnedSession(sessionId, trainer);

        if (session.getStatus() != TrainingSessionStatus.CANCELLED) {
            throw new SessionStateConflictException("Only a cancelled session can be restored.");
        }
        if (!SessionSchedule.firstStart(session).isAfter(LocalDateTime.now())) {
            throw new SessionStateConflictException(
                    "A cancelled session can only be restored before its first occurrence.");
        }

        validateSchedule(
                session.getStartDate(),
                session.getEndDate(),
                session.getStartTime(),
                session.getDays());

        session.setStatus(TrainingSessionStatus.SCHEDULED);
        session.setCancelledAt(null);
        session.setCancellationReason(null);
        session.setCurrentParticipants(0);
    }

    /**
     * Kept temporarily for existing clients. A booked business record is now
     * cancelled rather than physically deleted.
     */
    @Transactional
    public void deleteSession(Long sessionId, CancelTrainingSessionRequest request) {
        cancelSession(sessionId, request);
    }

    @Transactional(readOnly = true)
    public PageResponse<SessionParticipantResponse> getParticipants(Long sessionId, int page, int size) {
        User trainer = userService.getCurrentUser();
        TrainingSession session = getOwnedSession(sessionId, trainer);
        Pageable pageable = PageRequest.of(page, size);
        Page<SessionParticipantResponse> response =
                bookingRepository.findBySessionAndStatus(
                        session,
                        BookingStatus.CONFIRMED,
                        pageable)
                        .map(bookingMapper::toParticipantResponse);

        return new PageResponse<>(
                response.getContent(),
                response.getNumber(),
                response.getSize(),
                response.getTotalElements(),
                response.getTotalPages(),
                response.isFirst(),
                response.isLast());
    }

    private TrainingSession getOwnedSession(Long sessionId, User trainer) {
        TrainingSession session = trainingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Training session not found."));

        if (!session.getTrainer().getId().equals(trainer.getId())) {
            throw new ForbiddenOperationException("Only the trainer who created the session can manage it.");
        }
        return session;
    }

    private void requireScheduled(TrainingSession session) {
        if (session.getStatus() != TrainingSessionStatus.SCHEDULED) {
            throw new SessionStateConflictException(
                    "Only a scheduled training session can be updated or cancelled.");
        }
    }

    private void publishSessionChange(
            TrainingSession session,
            NotificationType type,
            String reason,
            List<Long> recipientIds) {
        if (!recipientIds.isEmpty()) {
            eventPublisher.publishEvent(new SessionChangedEvent(
                    session.getId(),
                    session.getTitle(),
                    type,
                    reason,
                    List.copyOf(recipientIds),
                    Instant.now()));
        }
    }

    private void validateSchedule(
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            Set<DayOfWeek> days) {
        if (startDate == null || endDate == null || startTime == null || days == null || days.isEmpty()) {
            throw new InvalidSessionScheduleException("The session schedule is incomplete.");
        }
        if (endDate.isBefore(startDate)) {
            throw new InvalidSessionScheduleException("End date cannot be before start date.");
        }

        LocalDateTime firstStart = SessionSchedule.firstStart(
                startDate,
                endDate,
                startTime,
                days);
        if (!firstStart.isAfter(LocalDateTime.now())) {
            throw new InvalidSessionScheduleException("The first training occurrence must be in the future.");
        }
    }

    private PageResponse<TrainingSessionResponse> toSessionPage(Page<TrainingSession> sessions) {
        Page<TrainingSessionResponse> response = sessions.map(trainingSessionMapper::toResponse);
        return new PageResponse<>(
                response.getContent(),
                response.getNumber(),
                response.getSize(),
                response.getTotalElements(),
                response.getTotalPages(),
                response.isFirst(),
                response.isLast());
    }
}
