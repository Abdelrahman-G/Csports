package com.csports.notification;

import java.time.Instant;
import java.util.List;

public record CourseChangedEvent(
        Long courseId,
        NotificationType type,
        String title,
        String content,
        List<Long> recipientIds,
        Instant occurredAt
) {}
