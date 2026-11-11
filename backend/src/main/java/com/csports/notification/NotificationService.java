package com.csports.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csports.common.exception.ResourceNotFoundException;
import com.csports.common.pagination.PageResponse;
import com.csports.notification.dto.NotificationResponse;
import com.csports.notification.dto.UnreadNotificationCountResponse;
import com.csports.user.User;
import com.csports.user.UserService;

@Service
public class NotificationService {

    private final UserNotificationRepository notificationRepository;
    private final UserService userService;
    private final com.csports.user.UserRepository userRepository;

    public NotificationService(
            UserNotificationRepository notificationRepository,
            UserService userService, com.csports.user.UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(int page, int size) {
        User currentUser = userService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<NotificationResponse> response = notificationRepository
                .findByRecipient(currentUser, pageable)
                .map(this::toResponse);

        return new PageResponse<>(
                response.getContent(),
                response.getNumber(),
                response.getSize(),
                response.getTotalElements(),
                response.getTotalPages(),
                response.isFirst(),
                response.isLast());
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getMyUnreadCount() {
        User currentUser = userService.getCurrentUser();
        return new UnreadNotificationCountResponse(
                notificationRepository.countByRecipientAndReadFalse(currentUser));
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        User currentUser = userService.getCurrentUser();
        UserNotification notification = notificationRepository
                .findByIdAndRecipient(notificationId, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found."));

        notification.setRead(true);
        return toResponse(notification);
    }

    private NotificationResponse toResponse(UserNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getCourseId(),
                notification.getAnnouncementId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt());
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void createCourseNotifications(Long courseId, Long announcementId, NotificationType type,
            String title, String content, java.util.List<Long> recipients) {
        int end = content.offsetByCodePoints(0, Math.min(240, content.codePointCount(0, content.length())));
        String preview = end < content.length() ? content.substring(0, end) + "…" : content;
        notificationRepository.saveAll(recipients.stream().distinct().map(id -> UserNotification.builder()
                .recipient(userRepository.getReferenceById(id)).courseId(courseId).announcementId(announcementId)
                .type(type).title(title).message(preview).build()).toList());
    }
}
