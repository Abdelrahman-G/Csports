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
                session.getCurrentParticipants(),
                session.getMaxParticipants()
        );
    }

    public TrainingSessionDetailsResponse toDetailsResponse(TrainingSession session, TrainerProfile trainerProfile) {

        return new TrainingSessionDetailsResponse(

                session.getId(),

                session.getTitle(),

                session.getDescription(),

                session.getTrainer().getName(),

                trainerProfile.getBio(),

                trainerProfile.getExperienceYears(),

                session.getSport().getName(),

                session.getLocationName(),

                session.getGoogleMapsUrl(),

                session.getStartDate(),

                session.getEndDate(),

                session.getStartTime(),

                session.getDurationMinutes(),

                Set.copyOf(session.getDays()),

                session.getPrice(),

                session.getCurrentParticipants(),

                session.getMaxParticipants(),

                session.getMaxParticipants() - session.getCurrentParticipants()
        );
    }

}
