package com.csports.booking.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import com.csports.booking.BookingStatus;
import com.csports.session.TrainingSessionStatus;

public record BookedSessionResponse(

        Long bookingId,

        BookingStatus bookingStatus,

        LocalDateTime bookedAt,

        LocalDateTime cancelledAt,

        Long sessionId,

        String title,

        Long trainerId,

        String trainerName,

        Long sportId,

        String sport,

        String locationName,

        Long regionId,

        String regionName,

        String city,

        String country,

        String googleMapsUrl,

        LocalDate startDate,

        LocalDate endDate,

        LocalTime startTime,

        Integer durationMinutes,

        Set<DayOfWeek> days,

        Double price,

        Integer currentParticipants,

        Integer maxParticipants,

        Integer remainingSeats,

        LocalDateTime bookingClosesAt,

        boolean bookingOpen,

        TrainingSessionStatus sessionStatus,

        String sessionCancellationReason

) {}
