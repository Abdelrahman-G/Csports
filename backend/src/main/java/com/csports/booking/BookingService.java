package com.csports.booking;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import com.csports.booking.dto.BookedSessionResponse;
import com.csports.common.pagination.PageResponse;
import com.csports.booking.exception.AlreadyBookedException;
import com.csports.booking.exception.CannotBookOwnSessionException;
import com.csports.common.exception.ResourceNotFoundException;
import com.csports.booking.exception.SessionFullException;
import com.csports.session.exception.TrainingSessionNotFoundException;
import com.csports.session.exception.SessionStateConflictException;
import com.csports.session.TrainingSession;
import com.csports.session.SessionSchedule;
import com.csports.session.TrainingSessionStatus;
import com.csports.user.User;
import com.csports.session.TrainingSessionRepository;
import com.csports.user.UserService;

import jakarta.transaction.Transactional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final UserService userService;
    private final BookingMapper bookingMapper;

    public BookingService(BookingRepository bookingRepository, TrainingSessionRepository trainingSessionRepository,
            UserService userService, BookingMapper bookingMapper) {

        this.bookingRepository = bookingRepository;
        this.trainingSessionRepository = trainingSessionRepository;
        this.userService = userService;
        this.bookingMapper = bookingMapper;
    }

    // Transactional annotation ensures that the entire booking process is treated
    // as a single transaction. If any part of the process fails (e.g., if the
    // session is full or the user has already booked), the entire transaction will
    // be rolled back, and no changes will be made to the database.
    // avoid booking the same last spot multiple times
    @Transactional
    public void bookSession(Long sessionId) {

        User user = userService.getCurrentUser();

        TrainingSession session = trainingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Training session not found"));

        if (session.getTrainer().getId().equals(user.getId())) {
            throw new CannotBookOwnSessionException();
        }

        if (session.getStatus() != TrainingSessionStatus.SCHEDULED) {
            throw new SessionStateConflictException("Only scheduled training sessions can be booked.");
        }

        if (!SessionSchedule.firstStart(session).isAfter(LocalDateTime.now())) {
            throw new SessionStateConflictException(
                    "Booking closes when the first training occurrence starts.");
        }

        if (bookingRepository.existsByUserAndSessionAndStatus(
                user,
                session,
                BookingStatus.CONFIRMED)) {
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
                .status(BookingStatus.CONFIRMED)
                .build();

        bookingRepository.save(booking);
    }

    @Transactional
    public PageResponse<BookedSessionResponse> getMySessions(int page, int size) {

        User currentUser = userService.getCurrentUser();

        Pageable pageable = PageRequest.of(page, size, Sort.by("bookedAt").descending());

        Page<Booking> bookings = bookingRepository.findByUserAndStatus(
                currentUser,
                BookingStatus.CONFIRMED,
                pageable);

        Page<BookedSessionResponse> response = bookings.map(bookingMapper::toResponse);

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
    public void cancelBooking(Long sessionId) {

        User currentUser = userService.getCurrentUser();

        TrainingSession session = trainingSessionRepository.findById(sessionId)
                .orElseThrow(TrainingSessionNotFoundException::new);

        Booking booking = bookingRepository.findByUserAndSessionAndStatus(
                        currentUser,
                        session,
                        BookingStatus.CONFIRMED)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));

        booking.setStatus(BookingStatus.CANCELLED_BY_USER);

        session.setCurrentParticipants(Math.max(0, session.getCurrentParticipants() - 1));
    }
}
