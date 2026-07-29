package com.csports.trainer;

import com.csports.common.web.ApiPaths;
import com.csports.common.pagination.PageResponse;
import com.csports.session.dto.TrainingSessionResponse;
import com.csports.session.TrainingSessionService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping({ApiPaths.TRAINERS, ApiPaths.LEGACY_TRAINERS})
@Validated
public class TrainerController {

    private final TrainingSessionService trainingSessionService;

    public TrainerController(TrainingSessionService trainingSessionService) {

        this.trainingSessionService = trainingSessionService;
    }

    @PreAuthorize("hasRole('TRAINER')")
    @GetMapping("/sessions")
    public PageResponse<TrainingSessionResponse> getMySessions(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        return trainingSessionService.getTrainerSessions(page,size);
    }
}
