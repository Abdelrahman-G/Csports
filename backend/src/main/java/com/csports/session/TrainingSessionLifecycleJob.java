package com.csports.session;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TrainingSessionLifecycleJob {

    private final TrainingSessionRepository trainingSessionRepository;

    public TrainingSessionLifecycleJob(TrainingSessionRepository trainingSessionRepository) {
        this.trainingSessionRepository = trainingSessionRepository;
    }

    /**
     * Marks a series complete only after its final actual occurrence ends.
     * Full sessions remain scheduled; capacity and lifecycle are separate.
     */
    @Scheduled(fixedDelayString = "${csports.session-completion-check-interval-ms:60000}")
    @Transactional
    public void completeEndedSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<TrainingSession> candidates =
                trainingSessionRepository.findByStatusAndEndDateLessThanEqual(
                        TrainingSessionStatus.SCHEDULED,
                        LocalDate.now());

        candidates.stream()
                .filter(session -> !SessionSchedule.finalEnd(session).isAfter(now))
                .forEach(session -> session.setStatus(TrainingSessionStatus.COMPLETED));
    }
}
