package com.csports.notification.dto;

import java.time.LocalDateTime;

import com.csports.notification.NotificationType;

public record NotificationResponse(
        Long id,
        Long sessionId,
        NotificationType type,
        String title,
        String message,
        boolean read,
        LocalDateTime createdAt
) {}
