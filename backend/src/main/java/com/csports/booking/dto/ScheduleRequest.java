package com.csports.booking.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record ScheduleRequest(
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

    @AssertTrue(message = "Choose a date range between 1 and 31 days; to is exclusive.")
    public boolean isValidRange() {
        return from == null || to == null
                || (ChronoUnit.DAYS.between(from, to) >= 1
                    && ChronoUnit.DAYS.between(from, to) <= 31);
    }
}
