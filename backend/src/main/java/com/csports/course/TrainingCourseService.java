package com.csports.course;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.cache.annotation.CacheEvict;
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
import com.csports.infrastructure.redis.CacheNames;
import com.csports.location.Region;
import com.csports.location.RegionRepository;
import com.csports.notification.NotificationType;
import com.csports.notification.CourseChangedEvent;
import com.csports.course.dto.CreateTrainingCourseRequest;
import com.csports.course.dto.CancelTrainingCourseRequest;
import com.csports.course.dto.CourseParticipantResponse;
import com.csports.course.dto.CourseSearchRequest;
import com.csports.course.dto.TrainingCourseDetailsResponse;
import com.csports.course.dto.TrainingCourseResponse;
import com.csports.course.dto.UpdateTrainingCourseRequest;
import com.csports.course.exception.InvalidCourseScheduleException;
import com.csports.course.exception.CourseStateConflictException;
import com.csports.course.exception.TrainingCourseNotFoundException;
import com.csports.sport.Sport;
import com.csports.trainer.TrainerProfile;
import com.csports.trainer.TrainerProfileRepository;
import com.csports.trainer.exception.TrainerProfileNotFoundException;
import com.csports.user.User;
import com.csports.user.UserService;

@Service
public class TrainingCourseService {

    private final TrainingCourseRepository trainingCourseRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final UserService userService;
    private final TrainingCourseMapper trainingCourseMapper;
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final RegionRepository regionRepository;
    private final TrainingCourseSearchService trainingCourseSearchService;

    public TrainingCourseService(
            TrainingCourseRepository trainingCourseRepository,
            TrainerProfileRepository trainerProfileRepository,
            UserService userService,
            TrainingCourseMapper trainingCourseMapper,
            BookingRepository bookingRepository,
            BookingMapper bookingMapper,
            ApplicationEventPublisher eventPublisher,
            RegionRepository regionRepository,
            TrainingCourseSearchService trainingCourseSearchService) {
        this.trainingCourseRepository = trainingCourseRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.userService = userService;
        this.trainingCourseMapper = trainingCourseMapper;
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.eventPublisher = eventPublisher;
        this.regionRepository = regionRepository;
        this.trainingCourseSearchService = trainingCourseSearchService;
    }

    @CacheEvict(cacheNames = CacheNames.COURSE_SEARCH, allEntries = true)
    @Transactional
    public TrainingCourseResponse createCourse(CreateTrainingCourseRequest request) {
        validateSchedule(request.startDate(), request.endDate(), request.startTime(), request.days());

        User trainer = userService.getCurrentUser();
        TrainerProfile trainerProfile = trainerProfileRepository.findByUser(trainer)
                .orElseThrow(TrainerProfileNotFoundException::new);

        Sport sport = trainerProfile.getSport();
        Region region = regionRepository.findById(request.regionId())
                .orElseThrow(() -> new ResourceNotFoundException("Region not found."));
        TrainingCourse course = TrainingCourse.builder()
                .trainer(trainer)
                .sport(sport)
                .region(region)
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
                .status(TrainingCourseStatus.SCHEDULED)
                .build();
        TrainingCourse savedCourse = trainingCourseRepository.save(course);
        return trainingCourseMapper.toResponse(savedCourse);
    }

    @Transactional(readOnly = true)
    public PageResponse<TrainingCourseResponse> getAllUpcomingCourses(int page, int size) {
        return searchCourses(new CourseSearchRequest(
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
                false,
                "startDate",
                "asc",
                page,
                size));
    }

    public PageResponse<TrainingCourseResponse> searchCourses(CourseSearchRequest request) {
        return trainingCourseSearchService.search(request);
    }

    @Transactional(readOnly = true)
    public TrainingCourseDetailsResponse getCourse(Long courseId) {
        TrainingCourse course = trainingCourseRepository.findById(courseId)
                .orElseThrow(TrainingCourseNotFoundException::new);

        TrainerProfile trainerProfile = trainerProfileRepository.findByUser(course.getTrainer())
                .orElseThrow(TrainerProfileNotFoundException::new);
        return trainingCourseMapper.toDetailsResponse(course, trainerProfile);
    }

    @Transactional(readOnly = true)
    public PageResponse<TrainingCourseResponse> getTrainerCourses(int page, int size) {
        User trainer = userService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").ascending());
        return toCoursePage(trainingCourseRepository.findByTrainer(trainer, pageable));
    }

    @CacheEvict(cacheNames = CacheNames.COURSE_SEARCH, allEntries = true)
    @Transactional
    public TrainingCourseResponse updateCourse(
            Long courseId,
            UpdateTrainingCourseRequest request) {
        User trainer = userService.getCurrentUser();
        TrainingCourse course = getOwnedCourse(courseId, trainer);
        requireScheduled(course);

        if (request.price() != null && Double.compare(request.price(), course.getPrice()) != 0) {
            throw new CourseStateConflictException(
                    "Course price cannot be changed after the course is created.");
        }
        if (!request.hasEditableFields()) {
            throw new InvalidCourseScheduleException(
                    "At least one editable course field must be provided.");
        }

        String updatedTitle = request.title() != null
                ? request.title()
                : course.getTitle();
        String updatedDescription = request.description() != null
                ? request.description()
                : course.getDescription();
        String updatedLocationName = request.locationName() != null
                ? request.locationName()
                : course.getLocationName();
        Region updatedRegion = request.regionId() != null
                ? regionRepository.findById(request.regionId())
                        .orElseThrow(() -> new ResourceNotFoundException("Region not found."))
                : course.getRegion();
        Double updatedLatitude = request.latitude() != null
                ? request.latitude()
                : course.getLatitude();
        Double updatedLongitude = request.longitude() != null
                ? request.longitude()
                : course.getLongitude();
        LocalDate updatedStartDate = request.startDate() != null
                ? request.startDate()
                : course.getStartDate();
        LocalDate updatedEndDate = request.endDate() != null
                ? request.endDate()
                : course.getEndDate();
        LocalTime updatedStartTime = request.startTime() != null
                ? request.startTime()
                : course.getStartTime();
        Integer updatedDurationMinutes = request.durationMinutes() != null
                ? request.durationMinutes()
                : course.getDurationMinutes();
        Set<DayOfWeek> updatedDays = request.days() != null
                ? Set.copyOf(request.days())
                : Set.copyOf(course.getDays());
        Integer updatedMaxParticipants = request.maxParticipants() != null
                ? request.maxParticipants()
                : course.getMaxParticipants();

        validateSchedule(
                updatedStartDate,
                updatedEndDate,
                updatedStartTime,
                updatedDays);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime updateDeadline = CourseSchedule.firstStart(course).minusDays(2);
        LocalDateTime requestedScheduleDeadline = CourseSchedule.firstStart(
                updatedStartDate,
                updatedEndDate,
                updatedStartTime,
                updatedDays).minusDays(2);
        if (!now.isBefore(updateDeadline) || !now.isBefore(requestedScheduleDeadline)) {
            throw new CourseStateConflictException(
                    "A course can only be updated when more than two days remain before both its current and updated first occurrence.");
        }

        if (updatedMaxParticipants < course.getCurrentParticipants()) {
            throw new CourseStateConflictException(
                    "Maximum participants cannot be lower than the number of existing bookings.");
        }

        boolean locationChanged =
                !Objects.equals(course.getLocationName(), updatedLocationName)
                || !Objects.equals(course.getRegion().getId(), updatedRegion.getId())
                || !Objects.equals(course.getLatitude(), updatedLatitude)
                || !Objects.equals(course.getLongitude(), updatedLongitude);

        course.setTitle(updatedTitle);
        course.setDescription(updatedDescription);
        course.setLocationName(updatedLocationName);
        course.setRegion(updatedRegion);
        course.setLatitude(updatedLatitude);
        course.setLongitude(updatedLongitude);
        course.setStartDate(updatedStartDate);
        course.setEndDate(updatedEndDate);
        course.setStartTime(updatedStartTime);
        course.setDurationMinutes(updatedDurationMinutes);
        course.setDays(updatedDays);
        course.setMaxParticipants(updatedMaxParticipants);
        course.setLastUpdateReason(request.reason());

        if (locationChanged) {
            publishCourseChange(
                    course,
                    NotificationType.COURSE_UPDATED,
                    request.reason(),
                    bookingRepository.findBookedUserIdsByCourseId(course.getId()));
        }

        return trainingCourseMapper.toResponse(course);
    }

    @CacheEvict(cacheNames = CacheNames.COURSE_SEARCH, allEntries = true)
    @Transactional
    public void cancelCourse(Long courseId, CancelTrainingCourseRequest request) {
        User trainer = userService.getCurrentUser();
        TrainingCourse course = getOwnedCourse(courseId, trainer);
        requireScheduled(course);

        List<Booking> activeBookings = bookingRepository.findAllByCourseAndStatus(
                course,
                BookingStatus.CONFIRMED);

        if (!activeBookings.isEmpty()) {
            LocalDate nextOccurrence = CourseSchedule.nextOccurrenceOnOrAfter(
                    course,
                    LocalDate.now());
            if (nextOccurrence == null) {
                throw new CourseStateConflictException(
                        "This course has no remaining occurrence to cancel.");
            }
            if (nextOccurrence.isEqual(LocalDate.now())) {
                throw new CourseStateConflictException(
                        "A course with active bookings cannot be cancelled on a training day.");
            }
        }

        LocalDateTime cancelledAt = LocalDateTime.now();
        course.setStatus(TrainingCourseStatus.CANCELLED);
        course.setCancelledAt(cancelledAt);
        course.setCancellationReason(request.reason());
        course.setCurrentParticipants(0);

        List<Long> recipientIds = activeBookings.stream()
                .map(booking -> booking.getUser().getId())
                .distinct()
                .toList();
        activeBookings.forEach(booking -> {
            booking.setStatus(BookingStatus.CANCELLED_BY_TRAINER);
            booking.setCancelledAt(cancelledAt);
        });

        publishCourseChange(
                course,
                NotificationType.COURSE_CANCELLED,
                request.reason(),
                recipientIds);
    }

    @CacheEvict(cacheNames = CacheNames.COURSE_SEARCH, allEntries = true)
    @Transactional
    public void restoreCourse(Long courseId) {
        User trainer = userService.getCurrentUser();
        TrainingCourse course = getOwnedCourse(courseId, trainer);

        if (course.getStatus() != TrainingCourseStatus.CANCELLED) {
            throw new CourseStateConflictException("Only a cancelled course can be restored.");
        }
        if (!CourseSchedule.firstStart(course).isAfter(LocalDateTime.now())) {
            throw new CourseStateConflictException(
                    "A cancelled course can only be restored before its first occurrence.");
        }

        validateSchedule(
                course.getStartDate(),
                course.getEndDate(),
                course.getStartTime(),
                course.getDays());

        course.setStatus(TrainingCourseStatus.SCHEDULED);
        course.setCancelledAt(null);
        course.setCancellationReason(null);
        course.setCurrentParticipants(0);
    }

    /**
     * Kept temporarily for existing clients. A booked business record is now
     * cancelled rather than physically deleted.
     */
    @CacheEvict(cacheNames = CacheNames.COURSE_SEARCH, allEntries = true)
    @Transactional
    public void deleteCourse(Long courseId, CancelTrainingCourseRequest request) {
        cancelCourse(courseId, request);
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseParticipantResponse> getParticipants(Long courseId, int page, int size) {
        User trainer = userService.getCurrentUser();
        TrainingCourse course = getOwnedCourse(courseId, trainer);
        Pageable pageable = PageRequest.of(page, size);
        Page<CourseParticipantResponse> response =
                bookingRepository.findByCourseAndStatus(
                        course,
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

    private TrainingCourse getOwnedCourse(Long courseId, User trainer) {
        TrainingCourse course = trainingCourseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Training course not found."));

        if (!course.getTrainer().getId().equals(trainer.getId())) {
            throw new ForbiddenOperationException("Only the trainer who created the course can manage it.");
        }
        return course;
    }

    private void requireScheduled(TrainingCourse course) {
        if (course.getStatus() != TrainingCourseStatus.SCHEDULED) {
            throw new CourseStateConflictException(
                    "Only a scheduled training course can be updated or cancelled.");
        }
    }

    private void publishCourseChange(
            TrainingCourse course,
            NotificationType type,
            String reason,
            List<Long> recipientIds) {
        if (!recipientIds.isEmpty()) {
            eventPublisher.publishEvent(new CourseChangedEvent(
                    course.getId(),
                    course.getTitle(),
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
            throw new InvalidCourseScheduleException("The course schedule is incomplete.");
        }
        if (endDate.isBefore(startDate)) {
            throw new InvalidCourseScheduleException("End date cannot be before start date.");
        }

        LocalDateTime firstStart = CourseSchedule.firstStart(
                startDate,
                endDate,
                startTime,
                days);
        if (!firstStart.isAfter(LocalDateTime.now())) {
            throw new InvalidCourseScheduleException("The first training occurrence must be in the future.");
        }
    }

    private PageResponse<TrainingCourseResponse> toCoursePage(Page<TrainingCourse> courses) {
        Page<TrainingCourseResponse> response = courses.map(trainingCourseMapper::toResponse);
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
