package com.csports.notification;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.csports.user.UserRepository;

@Component
public class SessionNotificationListener {

    private final UserNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public SessionNotificationListener(
            UserNotificationRepository notificationRepository,
            UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void createInAppNotifications(SessionChangedEvent event) {
        String title = event.type() == NotificationType.SESSION_CANCELLED
                ? "Training session cancelled"
                : "Training session updated";
        String message = event.type() == NotificationType.SESSION_CANCELLED
                ? "The training session \"" + event.sessionTitle()
                        + "\" was cancelled by its trainer. Reason: " + event.reason()
                : "The training session \"" + event.sessionTitle()
                        + "\" has a new location. Reason: " + event.reason();

        List<UserNotification> notifications = event.recipientIds().stream()
                .distinct()
                .map(userId -> UserNotification.builder()
                        .recipient(userRepository.getReferenceById(userId))
                        .sessionId(event.sessionId())
                        .type(event.type())
                        .title(title)
                        .message(message)
                        .build())
                .toList();

        notificationRepository.saveAll(notifications);
    }
}
// user :
// eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI2IiwiaWF0IjoxNzg1MzQ4NDUwLCJleHAiOjE3ODUzNDkzNTB9.Lw_ZJbNGsi4K2FCkBvP_C3hHshxtQUGB0_WLTs19emg
