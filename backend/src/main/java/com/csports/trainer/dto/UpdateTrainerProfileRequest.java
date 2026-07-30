package com.csports.trainer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateTrainerProfileRequest(
        @Size(max = 1000, message = "Bio must not exceed 1000 characters")
        String bio,

        @PositiveOrZero(message = "Experience years cannot be negative")
        @Max(value = 80, message = "Experience years must not exceed 80")
        Integer experienceYears
) {
    public boolean hasEditableFields() {
        return bio != null || experienceYears != null;
    }
}
