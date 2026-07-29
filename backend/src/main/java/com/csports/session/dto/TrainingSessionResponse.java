package com.csports.session.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.Set;

import com.csports.session.TrainingSessionStatus;

public record TrainingSessionResponse(

        Long id,

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

        Integer currentParticipants,

        Integer maxParticipants,

        TrainingSessionStatus status,

        LocalDateTime cancelledAt,

        String cancellationReason

) {}
