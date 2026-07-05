package com.Csports.Csports.mapper;

import org.springframework.stereotype.Component;

import com.Csports.Csports.DTO.BookedSessionResponse;
import com.Csports.Csports.model.Booking;
import com.Csports.Csports.model.TrainingSession;

@Component
public class BookingMapper {

    public BookedSessionResponse toResponse(Booking booking) {

        TrainingSession session = booking.getSession();

        return new BookedSessionResponse(

                session.getId(),

                session.getTitle(),

                session.getTrainer().getName(),

                session.getSport().getName(),

                session.getLocationName(),

                session.getGoogleMapsUrl(),

                session.getStartDate(),

                session.getEndDate(),

                session.getStartTime(),

                session.getDurationMinutes(),

                session.getDays(),

                session.getPrice(),

                booking.getBookedAt().toLocalDate()
        );
    }
}