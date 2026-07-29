package com.csports.session.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import com.csports.common.validation.ServiceArea;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateTrainingSessionRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @NotBlank(message = "Location name is required")
        @Size(max = 255, message = "Location name must not exceed 255 characters")
        String locationName,

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = ServiceArea.MIN_LATITUDE, message = "Latitude must be within the Cairo and Giza service area")
        @DecimalMax(value = ServiceArea.MAX_LATITUDE, message = "Latitude must be within the Cairo and Giza service area")
        Double latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = ServiceArea.MIN_LONGITUDE, message = "Longitude must be within the Cairo and Giza service area")
        @DecimalMax(value = ServiceArea.MAX_LONGITUDE, message = "Longitude must be within the Cairo and Giza service area")
        Double longitude,

        @NotNull(message = "Start date is required")
        @FutureOrPresent(message = "Start date cannot be in the past")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        @FutureOrPresent(message = "End date cannot be in the past")
        LocalDate endDate,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "Duration is required")
        @Positive(message = "Duration must be greater than zero")
        Integer durationMinutes,

        @NotEmpty(message = "At least one training day is required")
        Set<DayOfWeek> days,

        @NotNull(message = "Maximum participants is required")
        @Positive(message = "Maximum participants must be greater than zero")
        Integer maxParticipants,

        @NotNull(message = "Price is required")
        @PositiveOrZero(message = "Price cannot be negative")
        Double price

) {}
