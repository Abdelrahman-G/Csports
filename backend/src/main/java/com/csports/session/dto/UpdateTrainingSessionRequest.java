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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Partial session update. Reason is mandatory; every editable field is
 * optional and a null value means "leave the current value unchanged".
 */
public record UpdateTrainingSessionRequest(

        @NotBlank(message = "Update reason is required")
        @Size(max = 500, message = "Update reason must not exceed 500 characters")
        String reason,

        @Pattern(regexp = "(?s).*\\S.*", message = "Title must not be blank")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @Pattern(regexp = "(?s).*\\S.*", message = "Location name must not be blank")
        @Size(max = 255, message = "Location name must not exceed 255 characters")
        String locationName,

        @Positive(message = "Region id must be positive")
        Long regionId,

        @DecimalMin(value = ServiceArea.MIN_LATITUDE, message = "Latitude must be within the Cairo and Giza service area")
        @DecimalMax(value = ServiceArea.MAX_LATITUDE, message = "Latitude must be within the Cairo and Giza service area")
        Double latitude,

        @DecimalMin(value = ServiceArea.MIN_LONGITUDE, message = "Longitude must be within the Cairo and Giza service area")
        @DecimalMax(value = ServiceArea.MAX_LONGITUDE, message = "Longitude must be within the Cairo and Giza service area")
        Double longitude,

        @FutureOrPresent(message = "Start date cannot be in the past")
        LocalDate startDate,

        @FutureOrPresent(message = "End date cannot be in the past")
        LocalDate endDate,

        LocalTime startTime,

        @Positive(message = "Duration must be greater than zero")
        Integer durationMinutes,

        @Size(min = 1, message = "At least one training day is required")
        Set<DayOfWeek> days,

        @Positive(message = "Maximum participants must be greater than zero")
        Integer maxParticipants,

        /**
         * Optional consistency value. If supplied, it must equal the original
         * price; the service never writes it.
         */
        @PositiveOrZero(message = "Price cannot be negative")
        Double price
) {
    public boolean hasEditableFields() {
        return title != null
                || description != null
                || locationName != null
                || regionId != null
                || latitude != null
                || longitude != null
                || startDate != null
                || endDate != null
                || startTime != null
                || durationMinutes != null
                || days != null
                || maxParticipants != null;
    }
}
