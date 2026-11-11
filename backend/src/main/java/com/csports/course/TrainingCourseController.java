package com.csports.course;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.csports.common.pagination.PageResponse;
import com.csports.common.web.ApiPaths;
import com.csports.course.dto.CreateTrainingCourseRequest;
import com.csports.course.dto.CancelTrainingCourseRequest;
import com.csports.course.dto.CourseParticipantResponse;
import com.csports.course.dto.CourseSearchRequest;
import com.csports.course.dto.TrainingCourseDetailsResponse;
import com.csports.course.dto.TrainingCourseResponse;
import com.csports.course.dto.UpdateTrainingCourseRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping({ApiPaths.COURSES, ApiPaths.LEGACY_COURSES})
@Validated
public class TrainingCourseController {

    private final TrainingCourseService trainingCourseService;

    public TrainingCourseController(TrainingCourseService trainingCourseService) {
        this.trainingCourseService = trainingCourseService;
    }

    @PreAuthorize("hasRole('TRAINER')")
    @PostMapping
    public ResponseEntity<TrainingCourseResponse> createCourse(
            @Valid @RequestBody CreateTrainingCourseRequest request) {
        TrainingCourseResponse response = trainingCourseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public PageResponse<TrainingCourseResponse> getCourses(
            @Valid @ModelAttribute CourseSearchRequest request) {
        return trainingCourseService.searchCourses(request);
    }

    @GetMapping("/{courseId}")
    public TrainingCourseDetailsResponse getCourse(@PathVariable @Positive Long courseId) {
        return trainingCourseService.getCourse(courseId);
    }

    @PreAuthorize("hasRole('TRAINER')")
    @PatchMapping("/{courseId}")
    public ResponseEntity<TrainingCourseResponse> updateCourse(
            @PathVariable @Positive Long courseId,
            @Valid @RequestBody UpdateTrainingCourseRequest request) {
        TrainingCourseResponse updatedCourse =
                trainingCourseService.updateCourse(courseId, request);
        return ResponseEntity.ok(updatedCourse);
    }

    @PreAuthorize("hasRole('TRAINER')")
    @PatchMapping("/{courseId}/cancel")
    public ResponseEntity<String> cancelCourse(
            @PathVariable @Positive Long courseId,
            @Valid @RequestBody CancelTrainingCourseRequest request) {
        trainingCourseService.cancelCourse(courseId, request);
        return ResponseEntity.ok("Training course cancelled successfully.");
    }

    @PreAuthorize("hasRole('TRAINER')")
    @PatchMapping("/{courseId}/restore")
    public ResponseEntity<String> restoreCourse(@PathVariable @Positive Long courseId) {
        trainingCourseService.restoreCourse(courseId);
        return ResponseEntity.ok("Training course restored successfully.");
    }

    /**
     * Backward-compatible route for current clients. It now performs a safe
     * cancellation so bookings and notifications are preserved.
     */
    @PreAuthorize("hasRole('TRAINER')")
    @DeleteMapping("/{courseId}")
    public ResponseEntity<String> deleteCourse(
            @PathVariable @Positive Long courseId,
            @Valid @RequestBody CancelTrainingCourseRequest request) {
        trainingCourseService.deleteCourse(courseId, request);
        return ResponseEntity.ok("Training course cancelled successfully.");
    }

    @PreAuthorize("hasRole('TRAINER')")
    @GetMapping("/{courseId}/participants")
    public PageResponse<CourseParticipantResponse> getParticipants(
            @PathVariable @Positive Long courseId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return trainingCourseService.getParticipants(courseId, page, size);
    }
}
