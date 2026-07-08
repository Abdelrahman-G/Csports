package com.Csports.Csports.service;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Csports.Csports.DTO.CreateTrainingSessionRequest;
import com.Csports.Csports.DTO.PageResponse;
import com.Csports.Csports.DTO.TrainingSessionDetailsResponse;
import com.Csports.Csports.DTO.TrainingSessionResponse;
import com.Csports.Csports.exception.GeneralException;
import com.Csports.Csports.exception.ResourceNotFoundException;
import com.Csports.Csports.exception.TrainerProfileNotFoundException;
import com.Csports.Csports.exception.TrainingSessionNotFoundException;
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
        private final TrainingSessionMapper trainingSessionMapper;

        public TrainingSessionService(
                        TrainingSessionRepository trainingSessionRepository,
                        TrainerProfileRepository trainerProfileRepository,
                        UserService userService, TrainingSessionMapper trainingSessionMapper) {

                this.trainingSessionRepository = trainingSessionRepository;
                this.trainerProfileRepository = trainerProfileRepository;
                this.userService = userService;
                this.trainingSessionMapper = trainingSessionMapper;
        }

        public void createSession(CreateTrainingSessionRequest request) {

                User trainer = userService.getCurrentUser();
                TrainerProfile trainerProfile = trainerProfileRepository.findByUser(trainer)
                                .orElseThrow(TrainerProfileNotFoundException::new);

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

        public PageResponse<TrainingSessionResponse> getAllUpcomingSessions(int page, int size) {

                Pageable pageable = PageRequest.of(
                                page,
                                size,
                                Sort.by("startDate").ascending());

                Page<TrainingSession> sessions = trainingSessionRepository.findByStartDateGreaterThanEqual(
                                LocalDate.now(),
                                pageable);

                Page<TrainingSessionResponse> responsePage = sessions.map(trainingSessionMapper::toResponse);

                return new PageResponse<>(
                                responsePage.getContent(),
                                responsePage.getNumber(),
                                responsePage.getSize(),
                                responsePage.getTotalElements(),
                                responsePage.getTotalPages(),
                                responsePage.isFirst(),
                                responsePage.isLast());
        }

        @Transactional(readOnly = true)
        public TrainingSessionDetailsResponse getSession(Long sessionId) {
                TrainingSessionMapper mapper = new TrainingSessionMapper();
                TrainingSession session = trainingSessionRepository.findById(sessionId)
                                .orElseThrow(TrainingSessionNotFoundException::new);

                TrainerProfile trainerProfile = trainerProfileRepository.findByUser(session.getTrainer())
                                .orElseThrow(TrainerProfileNotFoundException::new);
                return mapper.toDetailsResponse(session, trainerProfile);
        }

        @Transactional
        public PageResponse<TrainingSessionResponse> getTrainerSessions(int page, int size) {
                TrainingSessionMapper mapper = new TrainingSessionMapper();
                User trainer = userService.getCurrentUser();

                Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").ascending());

                Page<TrainingSession> sessions = trainingSessionRepository.findByTrainer(trainer, pageable);

                Page<TrainingSessionResponse> response = sessions.map(mapper::toResponse);

                return new PageResponse<>(

                                response.getContent(),

                                response.getNumber(),

                                response.getSize(),

                                response.getTotalElements(),

                                response.getTotalPages(),

                                response.isFirst(),

                                response.isLast());
        }

        @Transactional
        public void deleteSession(Long sessionId) {

                User currentTrainer = userService.getCurrentUser();

                TrainingSession session = trainingSessionRepository.findById(sessionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Training session not found."));

                if (!session.getTrainer().getId().equals(currentTrainer.getId())) {
                        throw new GeneralException();
                }
                

                // notify users first (TODO)
                
                trainingSessionRepository.delete(session);
        }
}