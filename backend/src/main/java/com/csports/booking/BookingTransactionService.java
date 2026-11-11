package com.csports.booking;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csports.booking.dto.BookedCourseResponse;
import com.csports.booking.exception.AlreadyBookedException;
import com.csports.booking.exception.CannotBookOwnCourseException;
import com.csports.booking.exception.CourseFullException;
import com.csports.common.exception.ResourceNotFoundException;
import com.csports.course.CourseSchedule;
import com.csports.course.TrainingCourse;
import com.csports.course.TrainingCourseRepository;
import com.csports.course.TrainingCourseStatus;
import com.csports.course.exception.CourseStateConflictException;
import com.csports.course.exception.TrainingCourseNotFoundException;
import com.csports.user.User;
import com.csports.user.UserRepository;

/**
 * Owns the PostgreSQL transaction executed while the outer service holds the
 * Redis course lock. Because this is a separate Spring bean, the transaction
 * commits before control returns to the lock and releases it.
 */
@Service
public class BookingTransactionService {

    private final BookingRepository bookingRepository;
    private final TrainingCourseRepository trainingCourseRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;

    public BookingTransactionService(
            BookingRepository bookingRepository,
            TrainingCourseRepository trainingCourseRepository,
            UserRepository userRepository,
            BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.trainingCourseRepository = trainingCourseRepository;
        this.userRepository = userRepository;
        this.bookingMapper = bookingMapper;
    }

    @Transactional
    public BookedCourseResponse bookCourse(Long courseId, Long userId) {
        User user = getUser(userId);
        TrainingCourse course = getCourse(courseId);

        if (course.getTrainer().getId().equals(user.getId())) {
            throw new CannotBookOwnCourseException();
        }
        if (course.getStatus() != TrainingCourseStatus.SCHEDULED) {
            throw new CourseStateConflictException(
                    "Only scheduled training courses can be booked.");
        }
        if (!CourseSchedule.firstStart(course).isAfter(LocalDateTime.now())) {
            throw new CourseStateConflictException(
                    "Booking closes when the first training occurrence starts.");
        }
        if (bookingRepository.existsByUserAndCourseAndStatus(
                user,
                course,
                BookingStatus.CONFIRMED)) {
            throw new AlreadyBookedException();
        }
        if (course.getCurrentParticipants() >= course.getMaxParticipants()) {
            throw new CourseFullException();
        }

        course.setCurrentParticipants(course.getCurrentParticipants() + 1);
        Booking booking = Booking.builder()
                .user(user)
                .course(course)
                .status(BookingStatus.CONFIRMED)
                .build();

        // saveAndFlush forces PostgreSQL's @Version and unique-index checks to
        // happen before this method returns and before Redis is unlocked.
        Booking savedBooking = bookingRepository.saveAndFlush(booking);
        return bookingMapper.toResponse(savedBooking);
    }

    @Transactional
    public BookedCourseResponse cancelBooking(Long courseId, Long userId) {
        User user = getUser(userId);
        TrainingCourse course = getCourse(courseId);
        Booking booking = bookingRepository.findByUserAndCourseAndStatus(
                        user,
                        course,
                        BookingStatus.CONFIRMED)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));

        LocalDateTime now = LocalDateTime.now();
        if (course.getStatus() == TrainingCourseStatus.COMPLETED
                || !CourseSchedule.finalEnd(course).isAfter(now)) {
            throw new CourseStateConflictException(
                    "A booking cannot be cancelled after the course series is complete.");
        }
        if (course.getStatus() != TrainingCourseStatus.SCHEDULED) {
            throw new CourseStateConflictException(
                    "Only a booking for a scheduled course can be cancelled.");
        }
        if (course.getCurrentParticipants() <= 0) {
            throw new CourseStateConflictException(
                    "The stored participant count is inconsistent with this booking.");
        }

        booking.setStatus(BookingStatus.CANCELLED_BY_USER);
        booking.setCancelledAt(now);
        course.setCurrentParticipants(course.getCurrentParticipants() - 1);

        bookingRepository.flush();
        return bookingMapper.toResponse(booking);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found."));
    }

    private TrainingCourse getCourse(Long courseId) {
        return trainingCourseRepository.findById(courseId)
                .orElseThrow(TrainingCourseNotFoundException::new);
    }
}
