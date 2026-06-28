package com.Csports.Csports.service;

import org.springframework.stereotype.Service;

import com.Csports.Csports.DTO.CreateTrainingSessionRequest;
import com.Csports.Csports.model.Sport;
import com.Csports.Csports.model.TrainerProfile;
import com.Csports.Csports.model.TrainingSession;
import com.Csports.Csports.model.User;
import com.Csports.Csports.repository.SportRepository;
import com.Csports.Csports.repository.TrainerProfileRepository;
import com.Csports.Csports.repository.TrainingSessionRepository;

@Service
public class TrainingSessionService {

    private final TrainingSessionRepository trainingSessionRepository;
    private final SportRepository sportRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final UserService userService;

    public TrainingSessionService(
            TrainingSessionRepository trainingSessionRepository,
            SportRepository sportRepository,
            TrainerProfileRepository trainerProfileRepository,
            UserService userService) {

        this.trainingSessionRepository = trainingSessionRepository;
        this.sportRepository = sportRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.userService = userService;
    }

    public void createSession(CreateTrainingSessionRequest request) {

        User trainer = userService.getCurrentUser();

        Sport sport = sportRepository.findById(request.sportId())
                .orElseThrow(() -> new RuntimeException("Sport not found"));

        TrainerProfile trainerProfile = trainerProfileRepository.findByUser(userService.getCurrentUser())
        .orElseThrow(() -> new RuntimeException("Trainer profile not found"));

        if (trainerProfile == null) {
            throw new RuntimeException("Trainer profile not found.");
        }
        TrainingSession session = TrainingSession.builder()
                .trainer(trainer)
                .sport(sport)
                .description(request.description())
                .title(request.title())
                .locationName(request.locationName())
                .price(request.price())
                .maxParticipants(request.maxParticipants())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .startTime(request.startTime())
                .durationMinutes(request.durationMinutes())
                .days(request.days())
                .build();
        trainingSessionRepository.save(session);
    }
}