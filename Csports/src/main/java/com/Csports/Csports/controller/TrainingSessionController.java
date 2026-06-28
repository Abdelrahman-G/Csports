package com.Csports.Csports.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Csports.Csports.DTO.CreateTrainingSessionRequest;
import com.Csports.Csports.service.TrainingSessionService;

@RestController
@RequestMapping("/sessions")
public class TrainingSessionController {

    private final TrainingSessionService trainingSessionService;

    public TrainingSessionController(TrainingSessionService trainingSessionService) {
        this.trainingSessionService = trainingSessionService;
    }

    @PreAuthorize("hasRole('TRAINER')")
    @PostMapping
    public ResponseEntity<String> createSession(@RequestBody CreateTrainingSessionRequest request) {

        trainingSessionService.createSession(request);

        return ResponseEntity.ok("Training session created successfully.");
    }
}