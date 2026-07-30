package com.csports.booking;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.csports.booking.dto.BookedSessionResponse;
import com.csports.booking.dto.BookingSearchRequest;
import com.csports.common.pagination.PageResponse;
import com.csports.common.web.ApiPaths;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping({ApiPaths.BOOKINGS, ApiPaths.LEGACY_BOOKINGS})
@Validated
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {

        this.bookingService = bookingService;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{sessionId}")
    public ResponseEntity<BookedSessionResponse> bookSession(
            @PathVariable @Positive Long sessionId) {

        BookedSessionResponse booking = bookingService.bookSession(sessionId);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/me")
    public PageResponse<BookedSessionResponse> getMyBookings(
            @Valid @ModelAttribute BookingSearchRequest request) {

        return bookingService.getMyBookings(request);
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{sessionId}")
    public BookedSessionResponse cancelBooking(
            @PathVariable @Positive Long sessionId) {

        return bookingService.cancelBooking(sessionId);
    }
}
