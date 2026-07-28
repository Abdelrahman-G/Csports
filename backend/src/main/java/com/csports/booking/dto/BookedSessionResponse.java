package com.csports.booking.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record BookedSessionResponse(

        Long sessionId,

        String title,

        String trainerName,

        String sport,

        String locationName,

        String googleMapsUrl,

        LocalDate startDate,

        LocalDate endDate,

        LocalTime startTime,

        Integer durationMinutes,

        Set<DayOfWeek> days,

        Double price,

        LocalDate bookedAt

) {}