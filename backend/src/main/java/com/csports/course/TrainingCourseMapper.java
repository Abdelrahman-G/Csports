package com.csports.course;

import java.util.Set;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.csports.course.dto.TrainingCourseDetailsResponse;
import com.csports.course.dto.TrainingCourseResponse;
import com.csports.trainer.TrainerProfile;
import com.csports.course.TrainingCourse;

@Component
public class TrainingCourseMapper {

    public TrainingCourseResponse toResponse(TrainingCourse course) {
        return toResponse(course, null);
    }

    public TrainingCourseResponse toResponse(
            TrainingCourse course,
            Double distanceMeters) {
        LocalDateTime bookingClosesAt = CourseSchedule.firstStart(course);
        boolean bookingOpen = isBookingOpen(course, bookingClosesAt);

        return new TrainingCourseResponse(
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
                course.getCancelledAt(),
                course.getCancellationReason(),
                distanceMeters
        );
    }

    public TrainingCourseDetailsResponse toDetailsResponse(TrainingCourse course, TrainerProfile trainerProfile) {
        LocalDateTime bookingClosesAt = CourseSchedule.firstStart(course);
        boolean bookingOpen = isBookingOpen(course, bookingClosesAt);

        return new TrainingCourseDetailsResponse(

                course.getId(),

                course.getTitle(),

                course.getDescription(),

                course.getTrainer().getId(),

                course.getTrainer().getName(),

                trainerProfile.getBio(),

                trainerProfile.getExperienceYears(),

                course.getSport().getId(),

                course.getSport().getName(),

                course.getLocationName(),

                course.getRegion().getId(),

                course.getRegion().getName(),

                course.getRegion().getCity(),

                course.getRegion().getCountry(),

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

                course.getCancelledAt(),

                course.getLastUpdateReason(),

                course.getCancellationReason()
        );
    }

    private boolean isBookingOpen(
            TrainingCourse course,
            LocalDateTime bookingClosesAt) {
        return course.getStatus() == TrainingCourseStatus.SCHEDULED
                && course.getCurrentParticipants() < course.getMaxParticipants()
                && bookingClosesAt.isAfter(LocalDateTime.now());
    }
}
