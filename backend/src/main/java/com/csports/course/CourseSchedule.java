package com.csports.course;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.csports.course.exception.InvalidCourseScheduleException;

/**
 * Calculates actual occurrences inside a recurring course date range.
 */
public final class CourseSchedule {

    public static final ZoneId TIME_ZONE = ZoneId.of("Africa/Cairo");

    private CourseSchedule() {
    }

    public static List<CourseOccurrence> occurrencesBetween(
            TrainingCourse course, LocalDate from, LocalDate to) {
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
            throw new InvalidCourseScheduleException(
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
            throw new InvalidCourseScheduleException(
                    "The date range does not contain any of the selected training days.");
        }
        return candidate;
    }

    public static LocalDate nextOccurrenceOnOrAfter(TrainingCourse course, LocalDate date) {
        LocalDate candidate = date.isAfter(course.getStartDate()) ? date : course.getStartDate();
        while (!candidate.isAfter(course.getEndDate())
                && !course.getDays().contains(candidate.getDayOfWeek())) {
            candidate = candidate.plusDays(1);
        }
        return candidate.isAfter(course.getEndDate()) ? null : candidate;
    }

    public static LocalDateTime firstStart(
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            Set<DayOfWeek> days) {
        return LocalDateTime.of(firstOccurrence(startDate, endDate, days), startTime);
    }

    public static LocalDateTime firstStart(TrainingCourse course) {
        return firstStart(
                course.getStartDate(),
                course.getEndDate(),
                course.getStartTime(),
                course.getDays());
    }

    public static LocalDateTime finalEnd(TrainingCourse course) {
        LocalDate finalDate = lastOccurrence(
                course.getStartDate(),
                course.getEndDate(),
                course.getDays());
        return LocalDateTime.of(finalDate, course.getStartTime())
                .plusMinutes(course.getDurationMinutes());
    }
}
