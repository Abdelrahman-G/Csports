package com.csports.course;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TrainingCourseLifecycleJob {

    private final TrainingCourseRepository trainingCourseRepository;

    public TrainingCourseLifecycleJob(TrainingCourseRepository trainingCourseRepository) {
        this.trainingCourseRepository = trainingCourseRepository;
    }

    /**
     * Marks a series complete only after its final actual occurrence ends.
     * Full courses remain scheduled; capacity and lifecycle are separate.
     */
    @Scheduled(fixedDelayString = "${csports.course-completion-check-interval-ms:60000}")
    @Transactional
    public void completeEndedCourses() {
        LocalDateTime now = LocalDateTime.now();
        List<TrainingCourse> candidates =
                trainingCourseRepository.findByStatusAndEndDateLessThanEqual(
                        TrainingCourseStatus.SCHEDULED,
                        LocalDate.now());

        candidates.stream()
                .filter(course -> !CourseSchedule.finalEnd(course).isAfter(now))
                .forEach(course -> course.setStatus(TrainingCourseStatus.COMPLETED));
    }
}
