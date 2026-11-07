package com.csports.session;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.csports.session.exception.InvalidSessionScheduleException;

/**
 * Calculates actual occurrences inside a recurring session date range.
 */
public final class SessionSchedule {

    public static final ZoneId TIME_ZONE = ZoneId.of("Africa/Cairo");

    private SessionSchedule() {
    }

    public static List<CourseOccurrence> occurrencesBetween(
            TrainingSession course, LocalDate from, LocalDate to) {
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("The occurrence window must have a positive length.");
        }
        var windowStart = from.atStartOfDay(TIME_ZONE);
        var windowEnd = to.atStartOfDay(TIME_ZONE);
        // An earlier start can extend into this window, including across a DST transition.
        LocalDate earliestStart = windowStart.minusMinutes(course.getDurationMinutes()).toLocalDate();
        LocalDate candidate = nextOccurrenceOnOrAfter(course, earliestStart);
        List<CourseOccurrence> occurrences = new ArrayList<>();
        while (candidate != null && candidate.isBefore(to)) {
            var startsAt = candidate.atTime(course.getStartTime()).atZone(TIME_ZONE);
            var endsAt = startsAt.plusMinutes(course.getDurationMinutes());
            if (startsAt.isBefore(windowEnd) && endsAt.isAfter(windowStart)) {
                occurrences.add(new CourseOccurrence(startsAt, endsAt));
            }
            candidate = nextOccurrenceOnOrAfter(course, candidate.plusDays(1));
        }
        return List.copyOf(occurrences);
    }

    public static LocalDate firstOccurrence(
            LocalDate startDate,
            LocalDate endDate,
            Set<DayOfWeek> days) {
        LocalDate candidate = startDate;
        while (!candidate.isAfter(endDate) && !days.contains(candidate.getDayOfWeek())) {
            candidate = candidate.plusDays(1);
        }
        if (candidate.isAfter(endDate)) {
            throw new InvalidSessionScheduleException(
                    "The date range does not contain any of the selected training days.");
        }
        return candidate;
    }

    public static LocalDate lastOccurrence(
            LocalDate startDate,
            LocalDate endDate,
            Set<DayOfWeek> days) {
        LocalDate candidate = endDate;
        while (!candidate.isBefore(startDate) && !days.contains(candidate.getDayOfWeek())) {
            candidate = candidate.minusDays(1);
        }
        if (candidate.isBefore(startDate)) {
            throw new InvalidSessionScheduleException(
                    "The date range does not contain any of the selected training days.");
        }
        return candidate;
    }

    public static LocalDate nextOccurrenceOnOrAfter(TrainingSession session, LocalDate date) {
        LocalDate candidate = date.isAfter(session.getStartDate()) ? date : session.getStartDate();
        while (!candidate.isAfter(session.getEndDate())
                && !session.getDays().contains(candidate.getDayOfWeek())) {
            candidate = candidate.plusDays(1);
        }
        return candidate.isAfter(session.getEndDate()) ? null : candidate;
    }

    public static LocalDateTime firstStart(
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            Set<DayOfWeek> days) {
        return LocalDateTime.of(firstOccurrence(startDate, endDate, days), startTime);
    }

    public static LocalDateTime firstStart(TrainingSession session) {
        return firstStart(
                session.getStartDate(),
                session.getEndDate(),
                session.getStartTime(),
                session.getDays());
    }

    public static LocalDateTime finalEnd(TrainingSession session) {
        LocalDate finalDate = lastOccurrence(
                session.getStartDate(),
                session.getEndDate(),
                session.getDays());
        return LocalDateTime.of(finalDate, session.getStartTime())
                .plusMinutes(session.getDurationMinutes());
    }
}
