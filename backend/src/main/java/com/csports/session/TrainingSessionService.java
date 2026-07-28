package com.csports.session;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csports.session.dto.CreateTrainingSessionRequest;
import com.csports.common.pagination.PageResponse;
import com.csports.session.dto.SessionParticipantResponse;
import com.csports.session.dto.TrainingSessionDetailsResponse;
import com.csports.session.dto.TrainingSessionResponse;
import com.csports.common.exception.GeneralException;
import com.csports.common.exception.ResourceNotFoundException;
import com.csports.trainer.exception.TrainerProfileNotFoundException;
import com.csports.session.exception.TrainingSessionNotFoundException;
import com.csports.booking.BookingMapper;
import com.csports.session.TrainingSessionMapper;
import com.csports.booking.Booking;
import com.csports.sport.Sport;
import com.csports.trainer.TrainerProfile;
import com.csports.session.TrainingSession;
import com.csports.user.User;
import com.csports.booking.BookingRepository;
import com.csports.trainer.TrainerProfileRepository;
import com.csports.session.TrainingSessionRepository;
import com.csports.user.UserService;

@Service
public class TrainingSessionService {

        private final TrainingSessionRepository trainingSessionRepository;
        private final TrainerProfileRepository trainerProfileRepository;
        private final UserService userService;
        private final TrainingSessionMapper trainingSessionMapper;
        private final BookingRepository bookingRepository;
        private final BookingMapper bookingMapper;

        public TrainingSessionService(
                        TrainingSessionRepository trainingSessionRepository,
                        TrainerProfileRepository trainerProfileRepository,
                        UserService userService, TrainingSessionMapper trainingSessionMapper, BookingRepository bookingRepository, BookingMapper bookingMapper) {

                this.trainingSessionRepository = trainingSessionRepository;
                this.trainerProfileRepository = trainerProfileRepository;
                this.userService = userService;
                this.trainingSessionMapper = trainingSessionMapper;
                this.bookingRepository = bookingRepository;
                this.bookingMapper = bookingMapper;
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

        @Transactional(readOnly = true)
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
                TrainingSession session = trainingSessionRepository.findById(sessionId)
                                .orElseThrow(TrainingSessionNotFoundException::new);

                TrainerProfile trainerProfile = trainerProfileRepository.findByUser(session.getTrainer())
                                .orElseThrow(TrainerProfileNotFoundException::new);
                return trainingSessionMapper.toDetailsResponse(session, trainerProfile);
        }

        @Transactional
        public PageResponse<TrainingSessionResponse> getTrainerSessions(int page, int size) {
                User trainer = userService.getCurrentUser();

                Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").ascending());

                Page<TrainingSession> sessions = trainingSessionRepository.findByTrainer(trainer, pageable);

                Page<TrainingSessionResponse> response = sessions.map(trainingSessionMapper::toResponse);

                return new PageResponse<>(

                                response.getContent(),

                                response.getNumber(),

                                response.getSize(),

                                response.getTotalElements(),

                                response.getTotalPages(),

                                response.isFirst(),

                                response.isLast());
        }

        @Transactional(readOnly = true)
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

        @Transactional(readOnly = true)
        public PageResponse<SessionParticipantResponse> getParticipants(Long sessionId, int page, int size) {

                User trainer = userService.getCurrentUser();

                TrainingSession session = trainingSessionRepository.findById(sessionId)
                                .orElseThrow(TrainingSessionNotFoundException::new);

                if (!session.getTrainer().getId().equals(trainer.getId())) {
                        throw new GeneralException();
                }

                Pageable pageable = PageRequest.of(page, size);

                Page<Booking> bookings = bookingRepository.findBySession(session, pageable);

                Page<SessionParticipantResponse> response = bookings.map(bookingMapper::toParticipantResponse);

                return new PageResponse<>(
                                response.getContent(),
                                response.getNumber(),
                                response.getSize(),
                                response.getTotalElements(),
                                response.getTotalPages(),
                                response.isFirst(),
                                response.isLast());
        }
}
