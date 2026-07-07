package com.Csports.Csports.controller;

import com.Csports.Csports.DTO.PageResponse;
import com.Csports.Csports.DTO.TrainingSessionResponse;
import com.Csports.Csports.service.TrainingSessionService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trainers")
public class TrainerController {

    private final TrainingSessionService trainingSessionService;

    public TrainerController(TrainingSessionService trainingSessionService) {

        this.trainingSessionService = trainingSessionService;
    }

    @PreAuthorize("hasRole('TRAINER')")
    @GetMapping("/sessions")
    public PageResponse<TrainingSessionResponse> getMySessions(@RequestParam(defaultValue = "0")int page,@RequestParam(defaultValue = "10")int size) {

        return trainingSessionService.getTrainerSessions(page,size);
    }
}