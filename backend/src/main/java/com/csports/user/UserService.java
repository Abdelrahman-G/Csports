package com.csports.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.csports.booking.dto.BookedSessionResponse;
import com.csports.common.pagination.PageResponse;
import com.csports.booking.BookingMapper;
import com.csports.booking.Booking;
import com.csports.booking.BookingStatus;
import com.csports.user.User;
import com.csports.booking.BookingRepository;

@Service
public class UserService {
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;

    public UserService(BookingRepository bookingRepository, BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
    }

    public User getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return (User) authentication.getPrincipal();
    }

    @Transactional(readOnly = true)
    public PageResponse<BookedSessionResponse> getMySessions(int page, int size) {

        User currentUser = getCurrentUser();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("bookedAt").descending());

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

}
