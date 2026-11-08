package com.csports.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SessionScheduleTest {
    private TrainingSession course() {
        return TrainingSession.builder()
                .startDate(LocalDate.of(2026, 11, 1)).endDate(LocalDate.of(2026, 11, 30))
                .startTime(LocalTime.of(18, 0)).durationMinutes(90)
                .days(Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)).build();
    }

    @Test
    void expandsSelectedDaysInsideBothCourseAndExclusiveWindowBounds() {
        var occurrences = SessionSchedule.occurrencesBetween(course(), LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 14));
        assertThat(occurrences).extracting(o -> o.startsAt().toLocalDate())
                .containsExactly(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 12));
        assertThat(occurrences.getFirst().endsAt().toLocalTime()).isEqualTo(LocalTime.of(19, 30));
        assertThat(SessionSchedule.occurrencesBetween(course(), LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 8))).isEmpty();
        assertThat(SessionSchedule.occurrencesBetween(course(), LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 12))).hasSize(1);
    }

    @Test
    void retainsOvernightAndMultiDayOccurrencesStartingBeforeTheWindow() {
        var course = course();
        course.setDays(Set.of(DayOfWeek.FRIDAY));
        course.setStartTime(LocalTime.of(23, 30));
        course.setDurationMinutes(120);
        var occurrences = SessionSchedule.occurrencesBetween(course, LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 14));
        assertThat(occurrences).hasSize(2);
        assertThat(occurrences.getFirst().startsAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 6));
        course.setDurationMinutes(60 * 49);
        assertThat(SessionSchedule.occurrencesBetween(course, LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 9))).hasSize(1);
        course.setStartTime(LocalTime.of(23, 0));
        course.setDurationMinutes(60);
        assertThat(SessionSchedule.occurrencesBetween(course, LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 8))).isEmpty();
    }

    @Test
    void durationIsElapsedTimeAcrossCairoDaylightSaving() {
        var course = course();
        course.setStartDate(LocalDate.of(2026, 10, 29));
        course.setEndDate(LocalDate.of(2026, 10, 29));
        course.setStartTime(LocalTime.of(23, 30));
        course.setDurationMinutes(120);
        var occurrence = SessionSchedule.occurrencesBetween(course, LocalDate.of(2026, 10, 29), LocalDate.of(2026, 10, 31)).getFirst();
        assertThat(Duration.between(occurrence.startsAt(), occurrence.endsAt()).toMinutes()).isEqualTo(120);
        assertThat(occurrence.startsAt().getZone()).isEqualTo(SessionSchedule.TIME_ZONE);
    }

    @Test
    void rejectsEmptyWindowAndPreservesInclusiveDateSearch() {
        LocalDate day = LocalDate.of(2026, 11, 12);
        assertThatIllegalArgumentException().isThrownBy(() -> SessionSchedule.occurrencesBetween(course(), day, day));
        assertThat(SessionSchedule.nextOccurrenceOnOrAfter(course(), day)).isEqualTo(day);
        assertThat(SessionSchedule.nextOccurrenceOnOrAfter(course(), LocalDate.of(2026, 12, 1))).isNull();
    }
}
