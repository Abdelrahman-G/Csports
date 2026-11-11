package com.csports.booking;

import java.util.ArrayList;
import java.util.Comparator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csports.booking.dto.ScheduleRequest;
import com.csports.booking.dto.ScheduleResponse;
import com.csports.course.CourseSchedule;
import com.csports.course.TrainingCourseStatus;
import com.csports.user.UserService;

@Service
public class BookingScheduleService {
    private final BookingRepository bookingRepository;
    private final UserService userService;

    public BookingScheduleService(BookingRepository bookingRepository, UserService userService) {
        this.bookingRepository = bookingRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public ScheduleResponse getMySchedule(ScheduleRequest request) {
        Long userId = userService.getCurrentUser().getId();
        var entries = new ArrayList<ScheduleResponse.Entry>();
        for (Booking booking : bookingRepository.findScheduleCandidates(userId, request.to())) {
            var course = booking.getCourse();
            var enrolledAt = booking.getBookedAt().atZone(CourseSchedule.TIME_ZONE);
            var cancelledAt = booking.getCancelledAt();
            if (course.getCancelledAt() != null
                    && (cancelledAt == null || course.getCancelledAt().isBefore(cancelledAt))) {
                cancelledAt = course.getCancelledAt();
            }
            // Older cancelled records can lack a timestamp; do not invent active commitments or history.
            if (cancelledAt == null && (booking.getStatus() != BookingStatus.CONFIRMED
                    || course.getStatus() == TrainingCourseStatus.CANCELLED)) continue;
            for (var occurrence : CourseSchedule.occurrencesBetween(course, request.from(), request.to())) {
                if (occurrence.startsAt().isBefore(enrolledAt)) continue;
                if (cancelledAt != null
                        && !occurrence.startsAt().isBefore(cancelledAt.atZone(CourseSchedule.TIME_ZONE))) continue;
                entries.add(new ScheduleResponse.Entry(
                        booking.getId(), course.getId(), course.getTitle(), course.getSport().getName(),
                        course.getLocationName(),
                        occurrence.startsAt(), occurrence.endsAt()));
            }
        }
        entries.sort(Comparator.comparing(ScheduleResponse.Entry::startsAt)
                .thenComparing(ScheduleResponse.Entry::bookingId));
        return new ScheduleResponse(request.from(), request.to(), CourseSchedule.TIME_ZONE.getId(),
                java.time.Instant.now(), java.util.List.copyOf(entries));
    }
}
