package com.Csports.Csports.DTO;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

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

        Integer maxParticipants

) {}