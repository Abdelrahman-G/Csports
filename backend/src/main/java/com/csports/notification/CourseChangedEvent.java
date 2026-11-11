package com.csports.notification;

import java.time.Instant;
import java.util.List;

public record CourseChangedEvent(
        Long courseId,
        String courseTitle,
        NotificationType type,
        String reason,
        List<Long> recipientIds,
        Instant occurredAt
) {}
