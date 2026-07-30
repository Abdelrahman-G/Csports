package com.csports.session;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.csports.session.dto.TrainingSessionDetailsResponse;
import com.csports.session.dto.TrainingSessionResponse;
import com.csports.trainer.TrainerProfile;
import com.csports.session.TrainingSession;

@Component
public class TrainingSessionMapper {

    public TrainingSessionResponse toResponse(TrainingSession session) {

        return new TrainingSessionResponse(
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
                session.getStatus(),
                session.getCancelledAt(),
                session.getCancellationReason()
        );
    }

    public TrainingSessionDetailsResponse toDetailsResponse(TrainingSession session, TrainerProfile trainerProfile) {

        return new TrainingSessionDetailsResponse(

                session.getId(),

                session.getTitle(),

                session.getDescription(),

                session.getTrainer().getId(),

                session.getTrainer().getName(),

                trainerProfile.getBio(),

                trainerProfile.getExperienceYears(),

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

                session.getStatus(),

                session.getCancelledAt(),

                session.getLastUpdateReason(),

                session.getCancellationReason()
        );
    }

}
