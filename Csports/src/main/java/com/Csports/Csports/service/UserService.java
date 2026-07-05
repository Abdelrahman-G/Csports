package com.Csports.Csports.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.Csports.Csports.DTO.BookedSessionResponse;
import com.Csports.Csports.DTO.PageResponse;
import com.Csports.Csports.mapper.BookingMapper;
import com.Csports.Csports.model.Booking;
import com.Csports.Csports.model.User;
import com.Csports.Csports.repository.BookingRepository;

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
                Sort.by("bookedAt").descending()
        );

        Page<Booking> bookings =bookingRepository.findByUser(currentUser, pageable);

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