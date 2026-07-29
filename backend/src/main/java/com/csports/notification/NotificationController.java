package com.csports.notification;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.csports.common.pagination.PageResponse;
import com.csports.common.web.ApiPaths;
import com.csports.notification.dto.NotificationResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping({ApiPaths.NOTIFICATIONS, ApiPaths.LEGACY_NOTIFICATIONS})
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public PageResponse<NotificationResponse> getMyNotifications(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return notificationService.getMyNotifications(page, size);
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(@PathVariable @Positive Long notificationId) {
        return notificationService.markAsRead(notificationId);
    }
}
