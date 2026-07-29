package com.csports.booking;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.csports.booking.dto.BookedSessionResponse;
import com.csports.session.dto.SessionParticipantResponse;
import com.csports.user.User;
import com.csports.session.TrainingSession;

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

                Set.copyOf(session.getDays()),

                session.getPrice(),

                booking.getBookedAt().toLocalDate(),

                session.getStatus());
    }

    public SessionParticipantResponse toParticipantResponse(Booking booking) {

        User user = booking.getUser();

        return new SessionParticipantResponse(

                user.getId(),

                user.getName(),

                user.getEmail(),

                user.getPhoneNumber());
    }
}
