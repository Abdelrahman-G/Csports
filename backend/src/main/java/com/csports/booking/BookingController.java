package com.csports.booking;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.csports.booking.dto.BookedSessionResponse;
import com.csports.common.pagination.PageResponse;
import com.csports.booking.BookingService;
import com.csports.common.web.ApiPaths;

@RestController
@RequestMapping({ApiPaths.BOOKINGS, ApiPaths.LEGACY_BOOKINGS})
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {

        this.bookingService = bookingService;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{sessionId}")
    public ResponseEntity<String> bookSession(@PathVariable Long sessionId) {

        bookingService.bookSession(sessionId);

        return ResponseEntity.ok("Training session booked successfully.");
    }

    @GetMapping("/me")
    public PageResponse<BookedSessionResponse> getMySessions(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return bookingService.getMySessions(page, size);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<String> cancelBooking(@PathVariable Long sessionId) {

        bookingService.cancelBooking(sessionId);

        return ResponseEntity.ok("Booking cancelled successfully.");
    }
}
