package com.csports.booking;

import java.time.LocalDate;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csports.booking.dto.BookedCourseResponse;
import com.csports.booking.dto.BookingSearchRequest;
import com.csports.common.pagination.PageResponse;
import com.csports.infrastructure.redis.CacheNames;
import com.csports.user.User;
import com.csports.user.UserService;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final BookingMapper bookingMapper;
    private final CourseBookingLock courseBookingLock;
    private final BookingTransactionService bookingTransactionService;

    public BookingService(
            BookingRepository bookingRepository,
            UserService userService,
            BookingMapper bookingMapper,
            CourseBookingLock courseBookingLock,
            BookingTransactionService bookingTransactionService) {
        this.bookingRepository = bookingRepository;
        this.userService = userService;
        this.bookingMapper = bookingMapper;
        this.courseBookingLock = courseBookingLock;
        this.bookingTransactionService = bookingTransactionService;
    }

    @CacheEvict(cacheNames = CacheNames.COURSE_SEARCH, allEntries = true)
    public BookedCourseResponse bookCourse(Long courseId) {
        Long userId = userService.getCurrentUser().getId();
        return courseBookingLock.execute(
                courseId,
                () -> bookingTransactionService.bookCourse(courseId, userId));
    }

    @Transactional(readOnly = true)
    public PageResponse<BookedCourseResponse> getMyBookings(
            BookingSearchRequest request) {
        User currentUser = userService.getCurrentUser();
        PageRequest pageable = PageRequest.of(
                request.page(),
                request.size(),
                Sort.by(Sort.Direction.DESC, "bookedAt")
                        .and(Sort.by(Sort.Direction.DESC, "id")));

        Page<BookedCourseResponse> response = bookingRepository.findAll(
                        BookingSpecifications.forUser(
                                currentUser.getId(),
                                request,
                                LocalDate.now()),
                        pageable)
                .map(bookingMapper::toResponse);

        return new PageResponse<>(
                response.getContent(),
                response.getNumber(),
                response.getSize(),
                response.getTotalElements(),
                response.getTotalPages(),
                response.isFirst(),
                response.isLast());
    }

    /**
     * Backward-compatible service method for the deprecated users/courses
     * route. New clients should call bookings/me with explicit filters.
     */
    @Transactional(readOnly = true)
    public PageResponse<BookedCourseResponse> getMyCourses(int page, int size) {
        return getMyBookings(new BookingSearchRequest(
                null,
                BookingView.UPCOMING,
                null,
                page,
                size));
    }

    @CacheEvict(cacheNames = CacheNames.COURSE_SEARCH, allEntries = true)
    public BookedCourseResponse cancelBooking(Long courseId) {
        Long userId = userService.getCurrentUser().getId();
        return courseBookingLock.execute(
                courseId,
                () -> bookingTransactionService.cancelBooking(courseId, userId));
    }
}
