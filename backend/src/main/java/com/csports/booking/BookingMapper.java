package com.csports.booking;

import java.util.Set;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.csports.booking.dto.BookedCourseResponse;
import com.csports.course.dto.CourseParticipantResponse;
import com.csports.course.CourseSchedule;
import com.csports.course.TrainingCourseStatus;
import com.csports.user.User;
import com.csports.course.TrainingCourse;

@Component
public class BookingMapper {

    public BookedCourseResponse toResponse(Booking booking) {

        TrainingCourse course = booking.getCourse();
        LocalDateTime bookingClosesAt = CourseSchedule.firstStart(course);
        boolean bookingOpen = course.getStatus() == TrainingCourseStatus.SCHEDULED
                && course.getCurrentParticipants() < course.getMaxParticipants()
                && bookingClosesAt.isAfter(LocalDateTime.now());

        return new BookedCourseResponse(

                booking.getId(),

                booking.getStatus(),

                booking.getBookedAt(),

                booking.getCancelledAt(),

                course.getId(),

                course.getTitle(),

                course.getTrainer().getId(),

                course.getTrainer().getName(),

                course.getSport().getId(),

                course.getSport().getName(),

                course.getLocationName(),

                course.getRegion().getId(),

                course.getRegion().getName(),

                course.getRegion().getCity(),

                course.getRegion().getCountry(),

                booking.getStatus() == BookingStatus.CONFIRMED
                        ? course.getGoogleMapsUrl()
                        : null,

                course.getStartDate(),

                course.getEndDate(),

                course.getStartTime(),

                course.getDurationMinutes(),

                Set.copyOf(course.getDays()),

                course.getPrice(),

                course.getCurrentParticipants(),

                course.getMaxParticipants(),

                course.getMaxParticipants() - course.getCurrentParticipants(),

                bookingClosesAt,

                bookingOpen,

                course.getStatus(),

                course.getCancellationReason());
    }

    public CourseParticipantResponse toParticipantResponse(Booking booking) {

        User user = booking.getUser();

        return new CourseParticipantResponse(

                user.getId(),

                user.getName(),

                user.getEmail(),

                user.getPhoneNumber());
    }
}
