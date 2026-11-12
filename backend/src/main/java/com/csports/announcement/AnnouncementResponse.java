package com.csports.announcement;

import java.time.Instant;

public record AnnouncementResponse(Long id, Long courseId, Long authorId, String authorName,
        AnnouncementType type, String title, String content, Instant createdAt) {}
