package com.csports.booking;

import java.time.LocalDate;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csports.booking.dto.BookedSessionResponse;
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
    private final SessionBookingLock sessionBookingLock;
    private final BookingTransactionService bookingTransactionService;

    public BookingService(
            BookingRepository bookingRepository,
            UserService userService,
            BookingMapper bookingMapper,
            SessionBookingLock sessionBookingLock,
            BookingTransactionService bookingTransactionService) {
        this.bookingRepository = bookingRepository;
        this.userService = userService;
        this.bookingMapper = bookingMapper;
        this.sessionBookingLock = sessionBookingLock;
        this.bookingTransactionService = bookingTransactionService;
    }

    @CacheEvict(cacheNames = CacheNames.SESSION_SEARCH, allEntries = true)
    public BookedSessionResponse bookSession(Long sessionId) {
        Long userId = userService.getCurrentUser().getId();
        return sessionBookingLock.execute(
                sessionId,
                () -> bookingTransactionService.bookSession(sessionId, userId));
    }

    @Transactional(readOnly = true)
    public PageResponse<BookedSessionResponse> getMyBookings(
            BookingSearchRequest request) {
        User currentUser = userService.getCurrentUser();
        PageRequest pageable = PageRequest.of(
                request.page(),
                request.size(),
                Sort.by(Sort.Direction.DESC, "bookedAt")
                        .and(Sort.by(Sort.Direction.DESC, "id")));

        Page<BookedSessionResponse> response = bookingRepository.findAll(
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
     * Backward-compatible service method for the deprecated users/sessions
     * route. New clients should call bookings/me with explicit filters.
     */
    @Transactional(readOnly = true)
    public PageResponse<BookedSessionResponse> getMySessions(int page, int size) {
        return getMyBookings(new BookingSearchRequest(
                null,
                BookingView.UPCOMING,
                page,
                size));
    }

    @CacheEvict(cacheNames = CacheNames.SESSION_SEARCH, allEntries = true)
    public BookedSessionResponse cancelBooking(Long sessionId) {
        Long userId = userService.getCurrentUser().getId();
        return sessionBookingLock.execute(
                sessionId,
                () -> bookingTransactionService.cancelBooking(sessionId, userId));
    }
}
