package com.csports.booking.dto;

import com.csports.booking.BookingStatus;
import com.csports.booking.BookingView;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record BookingSearchRequest(
        BookingStatus status,
        BookingView view,

        @Min(value = 0, message = "Page must be zero or greater")
        Integer page,

        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size must not exceed 100")
        Integer size
) {
    public BookingSearchRequest {
        view = view == null ? BookingView.UPCOMING : view;
        page = page == null ? 0 : page;
        size = size == null ? 10 : size;
    }
}
