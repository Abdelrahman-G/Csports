package com.Csports.Csports.mapper;

import org.springframework.stereotype.Component;

import com.Csports.Csports.DTO.TrainingSessionDetailsResponse;
import com.Csports.Csports.DTO.TrainingSessionResponse;
import com.Csports.Csports.model.TrainerProfile;
import com.Csports.Csports.model.TrainingSession;

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
                session.getDays(),
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

                session.getDays(),

                session.getPrice(),

                session.getCurrentParticipants(),

                session.getMaxParticipants(),

                session.getMaxParticipants() - session.getCurrentParticipants()
        );
    }

}