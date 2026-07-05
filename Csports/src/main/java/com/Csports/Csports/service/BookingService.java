package com.Csports.Csports.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.Csports.Csports.DTO.BookedSessionResponse;
import com.Csports.Csports.DTO.PageResponse;
import com.Csports.Csports.exception.AlreadyBookedException;
import com.Csports.Csports.exception.CannotBookOwnSessionException;
import com.Csports.Csports.exception.ResourceNotFoundException;
import com.Csports.Csports.exception.SessionFullException;
import com.Csports.Csports.mapper.BookingMapper;
import com.Csports.Csports.model.Booking;
import com.Csports.Csports.model.TrainingSession;
import com.Csports.Csports.model.User;
import com.Csports.Csports.repository.BookingRepository;
import com.Csports.Csports.repository.TrainingSessionRepository;

import jakarta.transaction.Transactional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final UserService userService;
    private final BookingMapper bookingMapper;

    public BookingService(BookingRepository bookingRepository,TrainingSessionRepository trainingSessionRepository,UserService userService, BookingMapper bookingMapper) {

        this.bookingRepository = bookingRepository;
        this.trainingSessionRepository = trainingSessionRepository;
        this.userService = userService;
        this.bookingMapper = bookingMapper;
    }

    // Transactional annotation ensures that the entire booking process is treated as a single transaction. If any part of the process fails (e.g., if the session is full or the user has already booked), the entire transaction will be rolled back, and no changes will be made to the database.
    // avoid booking the same last spot multiple times
    @Transactional
    public void bookSession(Long sessionId) {

        User user = userService.getCurrentUser();

        TrainingSession session = trainingSessionRepository.findById(sessionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Training session not found"));

        if (session.getTrainer().getId().equals(user.getId())) {
            throw new CannotBookOwnSessionException();
        }

        if (bookingRepository.existsByUserAndSession(user, session)) {
            throw new AlreadyBookedException();
        }

        if (session.getCurrentParticipants() >= session.getMaxParticipants()) {
            throw new SessionFullException();
        }

        session.setCurrentParticipants(session.getCurrentParticipants() + 1);

        // Version is checked here
        trainingSessionRepository.save(session);

        Booking booking = Booking.builder()
                .user(user)
                .session(session)
                // .status(BookingStatus.CONFIRMED)
                .build();

        bookingRepository.save(booking);
    }

    @Transactional
    public PageResponse<BookedSessionResponse> getMySessions(int page, int size) {

        User currentUser = userService.getCurrentUser();

        Pageable pageable = PageRequest.of(page, size, Sort.by("bookedAt").descending());

        Page<Booking> bookings = bookingRepository.findByUser(currentUser, pageable);

        Page<BookedSessionResponse> response = bookings.map(bookingMapper::toResponse);

        return new PageResponse<>(
                response.getContent(),
                response.getNumber(),
                response.getSize(),
                response.getTotalElements(),
                response.getTotalPages(),
                response.isFirst(),
                response.isLast()
        );
    }
}
