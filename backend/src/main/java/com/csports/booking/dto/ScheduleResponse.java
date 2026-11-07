package com.csports.booking.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

public record ScheduleResponse(
        LocalDate from, LocalDate to, String timeZone, Instant serverNow,
        List<Entry> entries) {

    public record Entry(
            Long bookingId, Long courseId, String title, String sport,
            String trainerName, String locationName, String regionName,
            ZonedDateTime startsAt, ZonedDateTime endsAt) {}
}
