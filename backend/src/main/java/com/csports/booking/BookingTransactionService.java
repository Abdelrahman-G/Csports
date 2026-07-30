package com.csports.booking;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csports.booking.dto.BookedSessionResponse;
import com.csports.booking.exception.AlreadyBookedException;
import com.csports.booking.exception.CannotBookOwnSessionException;
import com.csports.booking.exception.SessionFullException;
import com.csports.common.exception.ResourceNotFoundException;
import com.csports.session.SessionSchedule;
import com.csports.session.TrainingSession;
import com.csports.session.TrainingSessionRepository;
import com.csports.session.TrainingSessionStatus;
import com.csports.session.exception.SessionStateConflictException;
import com.csports.session.exception.TrainingSessionNotFoundException;
import com.csports.user.User;
import com.csports.user.UserRepository;

/**
 * Owns the PostgreSQL transaction executed while the outer service holds the
 * Redis session lock. Because this is a separate Spring bean, the transaction
 * commits before control returns to the lock and releases it.
 */
@Service
public class BookingTransactionService {

    private final BookingRepository bookingRepository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;

    public BookingTransactionService(
            BookingRepository bookingRepository,
            TrainingSessionRepository trainingSessionRepository,
            UserRepository userRepository,
            BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.trainingSessionRepository = trainingSessionRepository;
        this.userRepository = userRepository;
        this.bookingMapper = bookingMapper;
    }

    @Transactional
    public BookedSessionResponse bookSession(Long sessionId, Long userId) {
        User user = getUser(userId);
        TrainingSession session = getSession(sessionId);

        if (session.getTrainer().getId().equals(user.getId())) {
            throw new CannotBookOwnSessionException();
        }
        if (session.getStatus() != TrainingSessionStatus.SCHEDULED) {
            throw new SessionStateConflictException(
                    "Only scheduled training sessions can be booked.");
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
        Booking booking = Booking.builder()
                .user(user)
                .session(session)
                .status(BookingStatus.CONFIRMED)
                .build();

        // saveAndFlush forces PostgreSQL's @Version and unique-index checks to
        // happen before this method returns and before Redis is unlocked.
        Booking savedBooking = bookingRepository.saveAndFlush(booking);
        return bookingMapper.toResponse(savedBooking);
    }

    @Transactional
    public BookedSessionResponse cancelBooking(Long sessionId, Long userId) {
        User user = getUser(userId);
        TrainingSession session = getSession(sessionId);
        Booking booking = bookingRepository.findByUserAndSessionAndStatus(
                        user,
                        session,
                        BookingStatus.CONFIRMED)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));

        LocalDateTime now = LocalDateTime.now();
        if (session.getStatus() == TrainingSessionStatus.COMPLETED
                || !SessionSchedule.finalEnd(session).isAfter(now)) {
            throw new SessionStateConflictException(
                    "A booking cannot be cancelled after the session series is complete.");
        }
        if (session.getStatus() != TrainingSessionStatus.SCHEDULED) {
            throw new SessionStateConflictException(
                    "Only a booking for a scheduled session can be cancelled.");
        }
        if (session.getCurrentParticipants() <= 0) {
            throw new SessionStateConflictException(
                    "The stored participant count is inconsistent with this booking.");
        }

        booking.setStatus(BookingStatus.CANCELLED_BY_USER);
        booking.setCancelledAt(now);
        session.setCurrentParticipants(session.getCurrentParticipants() - 1);

        bookingRepository.flush();
        return bookingMapper.toResponse(booking);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found."));
    }

    private TrainingSession getSession(Long sessionId) {
        return trainingSessionRepository.findById(sessionId)
                .orElseThrow(TrainingSessionNotFoundException::new);
    }
}
