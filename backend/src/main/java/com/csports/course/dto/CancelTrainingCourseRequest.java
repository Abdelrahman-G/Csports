package com.csports.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelTrainingCourseRequest(
        @NotBlank(message = "Cancellation reason is required")
        @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
        String reason
) {}
