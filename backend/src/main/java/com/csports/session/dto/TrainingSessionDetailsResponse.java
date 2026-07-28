package com.csports.session.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record TrainingSessionDetailsResponse(

        Long id,

        String title,

        String description,

        String trainerName,

        String trainerBio,

        Integer trainerExperienceYears,

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

        Integer remainingSeats
) {}