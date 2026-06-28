package com.Csports.Csports.DTO;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record CreateTrainingSessionRequest(

        String title,

        String description,

        Long sportId,

        String locationName,

        Double latitude,

        Double longitude,

        LocalDate startDate,

        LocalDate endDate,

        LocalTime startTime,

        Integer durationMinutes,

        Set<DayOfWeek> days,

        Integer maxParticipants,

        Double price

) {}