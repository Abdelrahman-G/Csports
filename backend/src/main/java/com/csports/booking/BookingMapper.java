package com.csports.booking;

import java.util.Set;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.csports.booking.dto.BookedSessionResponse;
import com.csports.session.dto.SessionParticipantResponse;
import com.csports.session.SessionSchedule;
import com.csports.session.TrainingSessionStatus;
import com.csports.user.User;
import com.csports.session.TrainingSession;

@Component
public class BookingMapper {

    public BookedSessionResponse toResponse(Booking booking) {

        TrainingSession session = booking.getSession();
        LocalDateTime bookingClosesAt = SessionSchedule.firstStart(session);
        boolean bookingOpen = session.getStatus() == TrainingSessionStatus.SCHEDULED
                && session.getCurrentParticipants() < session.getMaxParticipants()
                && bookingClosesAt.isAfter(LocalDateTime.now());

        return new BookedSessionResponse(

                booking.getId(),

                booking.getStatus(),

                booking.getBookedAt(),

                booking.getCancelledAt(),

                session.getId(),

                session.getTitle(),

                session.getTrainer().getId(),

                session.getTrainer().getName(),

                session.getSport().getId(),

                session.getSport().getName(),

                session.getLocationName(),

                session.getRegion().getId(),

                session.getRegion().getName(),

                session.getRegion().getCity(),

                session.getRegion().getCountry(),

                session.getGoogleMapsUrl(),

                session.getStartDate(),

                session.getEndDate(),

                session.getStartTime(),

                session.getDurationMinutes(),

                Set.copyOf(session.getDays()),

                session.getPrice(),

                session.getCurrentParticipants(),

                session.getMaxParticipants(),

                session.getMaxParticipants() - session.getCurrentParticipants(),

                bookingClosesAt,

                bookingOpen,

                session.getStatus(),

                session.getCancellationReason());
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
