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
import com.csports.user.User;
import com.csports.user.UserService;

@Service
public class NotificationService {

    private final UserNotificationRepository notificationRepository;
    private final UserService userService;

    public NotificationService(
            UserNotificationRepository notificationRepository,
            UserService userService) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
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
                notification.getSessionId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
