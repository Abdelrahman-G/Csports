package com.Csports.Csports.service;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.Csports.Csports.DTO.CreateTrainingSessionRequest;
import com.Csports.Csports.DTO.TrainingSessionResponse;
import com.Csports.Csports.exception.TrainerProfileNotFoundException;
import com.Csports.Csports.mapper.TrainingSessionMapper;
import com.Csports.Csports.model.Sport;
import com.Csports.Csports.model.TrainerProfile;
import com.Csports.Csports.model.TrainingSession;
import com.Csports.Csports.model.User;
import com.Csports.Csports.repository.TrainerProfileRepository;
import com.Csports.Csports.repository.TrainingSessionRepository;

@Service
public class TrainingSessionService {

    private final TrainingSessionRepository trainingSessionRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final UserService userService;

    public TrainingSessionService(
            TrainingSessionRepository trainingSessionRepository,
            TrainerProfileRepository trainerProfileRepository,
            UserService userService) {

        this.trainingSessionRepository = trainingSessionRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.userService = userService;
    }

    public void createSession(CreateTrainingSessionRequest request) {

        User trainer = userService.getCurrentUser();
        TrainerProfile trainerProfile = trainerProfileRepository.findByUser(trainer).orElseThrow(TrainerProfileNotFoundException::new);

        Sport sport = trainerProfile.getSport();
        TrainingSession session = TrainingSession.builder()
                .trainer(trainer)
                .sport(sport)
                .description(request.description())
                .latitude(request.latitude())
                .longitude(request.longitude())
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

    public Page<TrainingSessionResponse> getAllUpcomingSessions(int page,int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").ascending());

        TrainingSessionMapper mapper = new TrainingSessionMapper();
        return trainingSessionRepository.findByStartDateGreaterThanEqual(LocalDate.now(), pageable)
                .map(mapper::toResponse);
    }
}