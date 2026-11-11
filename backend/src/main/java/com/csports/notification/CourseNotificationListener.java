package com.csports.notification;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.csports.user.UserRepository;

@Component
public class CourseNotificationListener {

    private final UserNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public CourseNotificationListener(
            UserNotificationRepository notificationRepository,
            UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void createInAppNotifications(CourseChangedEvent event) {
        String title = event.type() == NotificationType.COURSE_CANCELLED
                ? "Training course cancelled"
                : "Training course updated";
        String message = event.type() == NotificationType.COURSE_CANCELLED
                ? "The training course \"" + event.courseTitle()
                        + "\" was cancelled by its trainer. Reason: " + event.reason()
                : "The training course \"" + event.courseTitle()
                        + "\" has a new location. Reason: " + event.reason();

        List<UserNotification> notifications = event.recipientIds().stream()
                .distinct()
                .map(userId -> UserNotification.builder()
                        .recipient(userRepository.getReferenceById(userId))
                        .courseId(event.courseId())
                        .type(event.type())
                        .title(title)
                        .message(message)
                        .build())
                .toList();

        notificationRepository.saveAll(notifications);
    }
}
