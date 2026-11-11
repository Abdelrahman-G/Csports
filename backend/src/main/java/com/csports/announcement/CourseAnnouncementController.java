package com.csports.announcement;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.csports.common.web.ApiPaths;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@RestController
@Validated
@RequestMapping(ApiPaths.COURSES + "/{courseId}/announcements")
@PreAuthorize("hasAnyRole('USER', 'TRAINER')")
public class CourseAnnouncementController {
    private final CourseAnnouncementService service;

    public CourseAnnouncementController(CourseAnnouncementService service) { this.service = service; }

    @GetMapping
    public NoticeboardResponse list(@PathVariable @Positive Long courseId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return service.getNoticeboard(courseId, page, size);
    }

    @GetMapping("/{announcementId}")
    public AnnouncementResponse get(@PathVariable @Positive Long courseId,
            @PathVariable @Positive Long announcementId) {
        return service.getAnnouncement(courseId, announcementId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TRAINER')")
    public AnnouncementResponse publish(@PathVariable @Positive Long courseId,
            @Valid @RequestBody PublishAnnouncementRequest request) {
        return service.publish(courseId, request);
    }
}
