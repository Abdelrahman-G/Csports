package com.csports.notification;

import java.time.Instant;
import java.util.List;

public record SessionChangedEvent(
        Long sessionId,
        String sessionTitle,
        NotificationType type,
        String reason,
        List<Long> recipientIds,
        Instant occurredAt
) {}
