package com.csports.announcement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublishAnnouncementRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 2000) String content) {
    public PublishAnnouncementRequest {
        title = title == null ? null : title.strip();
        content = content == null ? null : content.strip();
    }
}
