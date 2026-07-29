package com.csports.trainer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateTrainerProfileRequest(

        @Size(max = 1000, message = "Bio must not exceed 1000 characters")
        String bio,

        @NotNull(message = "Experience years is required")
        @PositiveOrZero(message = "Experience years cannot be negative")
        @Max(value = 80, message = "Experience years must not exceed 80")
        Integer experienceYears,

        @NotNull(message = "Sport is required")
        @Positive(message = "Sport id must be positive")
        Long sportId

) {}
