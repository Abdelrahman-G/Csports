package com.csports.announcement;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.csports.booking.BookingRepository;
import com.csports.booking.BookingStatus;
import com.csports.common.exception.ForbiddenOperationException;
import com.csports.common.exception.ResourceNotFoundException;
import com.csports.common.pagination.PageResponse;
import com.csports.course.*;
import com.csports.course.exception.CourseStateConflictException;
import com.csports.notification.CourseChangedEvent;
import com.csports.notification.NotificationService;
import com.csports.notification.NotificationType;
import com.csports.user.User;
import com.csports.user.UserService;

@Service
public class CourseAnnouncementService {
    private final CourseAnnouncementRepository announcements;
    private final TrainingCourseRepository courses;
    private final BookingRepository bookings;
    private final UserService users;
    private final NotificationService notifications;

    public CourseAnnouncementService(CourseAnnouncementRepository announcements,
            TrainingCourseRepository courses, BookingRepository bookings,
            UserService users, NotificationService notifications) {
        this.announcements = announcements;
        this.courses = courses;
        this.bookings = bookings;
        this.users = users;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public NoticeboardResponse getNoticeboard(Long courseId, int page, int size) {
        TrainingCourse course = readableCourse(courseId);
        var results = announcements.findResponsesByCourseId(courseId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return new NoticeboardResponse(isOwner(course) && isOpen(course), new PageResponse<>(
                results.getContent(), results.getNumber(), results.getSize(), results.getTotalElements(),
                results.getTotalPages(), results.isFirst(), results.isLast()));
    }

    @Transactional(readOnly = true)
    public AnnouncementResponse getAnnouncement(Long courseId, Long announcementId) {
        readableCourse(courseId);
        return announcements.findResponse(courseId, announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found."));
    }

    @Transactional
    public AnnouncementResponse publish(Long courseId, PublishAnnouncementRequest request) {
        // Hold the course row until the post and alerts commit, serializing against cancellation.
        TrainingCourse course = courses.findForAnnouncement(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found."));
        if (!isOwner(course)) throw new ForbiddenOperationException("Only this course's trainer can publish announcements.");
        if (!isOpen(course)) throw new CourseStateConflictException("Announcements are closed for this course.");
        return record(course, users.getCurrentUser(), AnnouncementType.TRAINER_MESSAGE,
                request.title(), request.content(), Instant.now(), bookings.findBookedUserIdsByCourseId(courseId));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordCourseChange(CourseChangedEvent event) {
        TrainingCourse course = courses.findById(event.courseId()).orElseThrow();
        record(course, course.getTrainer(), event.type() == NotificationType.COURSE_CANCELLED
                ? AnnouncementType.COURSE_CANCELLED : AnnouncementType.COURSE_UPDATED,
                event.title(), event.content(), event.occurredAt(), event.recipientIds());
    }

    private AnnouncementResponse record(TrainingCourse course, User author, AnnouncementType type,
            String title, String content, Instant createdAt, List<Long> recipients) {
        var announcement = announcements.save(new CourseAnnouncement(course, author, type, title, content, createdAt));
        NotificationType notificationType = switch (type) {
            case TRAINER_MESSAGE -> NotificationType.COURSE_ANNOUNCEMENT;
            case COURSE_UPDATED -> NotificationType.COURSE_UPDATED;
            case COURSE_CANCELLED -> NotificationType.COURSE_CANCELLED;
        };
        notifications.createCourseNotifications(course.getId(), announcement.getId(), notificationType, title, content, recipients);
        return new AnnouncementResponse(announcement.getId(), course.getId(), author.getId(), author.getName(),
                type, title, content, createdAt);
    }

    private TrainingCourse readableCourse(Long courseId) {
        TrainingCourse course = courses.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found."));
        if (!isOwner(course) && !bookings.existsByUserAndCourseAndStatus(users.getCurrentUser(), course, BookingStatus.CONFIRMED)) {
            throw new ForbiddenOperationException("The noticeboard is available to this course's registered trainees.");
        }
        return course;
    }

    private boolean isOwner(TrainingCourse course) {
        return course.getTrainer().getId().equals(users.getCurrentUser().getId());
    }

    private boolean isOpen(TrainingCourse course) {
        return course.getStatus() == TrainingCourseStatus.SCHEDULED
                && CourseSchedule.finalEnd(course).isAfter(LocalDateTime.now(CourseSchedule.TIME_ZONE));
    }
}
