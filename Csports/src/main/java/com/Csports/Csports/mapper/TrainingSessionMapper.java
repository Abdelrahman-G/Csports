package com.Csports.Csports.mapper;

import org.springframework.stereotype.Component;
import com.Csports.Csports.DTO.TrainingSessionResponse;
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
}