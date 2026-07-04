package com.Csports.Csports.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Csports.Csports.service.BookingService;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(
            BookingService bookingService) {

        this.bookingService = bookingService;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{sessionId}")
    public ResponseEntity<String> bookSession(@PathVariable Long sessionId) {

        bookingService.bookSession(sessionId);

        return ResponseEntity.ok("Training session booked successfully.");
    }
}