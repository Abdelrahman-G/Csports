package com.csports.trainer;

import com.csports.common.web.ApiPaths;
import com.csports.common.pagination.PageResponse;
import com.csports.session.dto.TrainingSessionResponse;
import com.csports.session.TrainingSessionService;
import com.csports.trainer.dto.TrainerProfileResponse;
import com.csports.trainer.dto.UpdateTrainerProfileRequest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;

@RestController
@RequestMapping({ApiPaths.TRAINERS, ApiPaths.LEGACY_TRAINERS})
@Validated
public class TrainerController {

    private final TrainingSessionService trainingSessionService;
    private final TrainerProfileService trainerProfileService;

    public TrainerController(
            TrainingSessionService trainingSessionService,
            TrainerProfileService trainerProfileService) {
        this.trainingSessionService = trainingSessionService;
        this.trainerProfileService = trainerProfileService;
    }

    @PreAuthorize("hasRole('TRAINER')")
    @GetMapping("/me")
    public TrainerProfileResponse getMyProfile() {
        return trainerProfileService.getMyProfile();
    }

    @PreAuthorize("hasRole('TRAINER')")
    @PatchMapping("/me")
    public TrainerProfileResponse updateMyProfile(
            @Valid @RequestBody UpdateTrainerProfileRequest request) {
        return trainerProfileService.updateMyProfile(request);
    }

    @GetMapping("/{trainerId}")
    public TrainerProfileResponse getPublicProfile(
            @PathVariable @Positive Long trainerId) {
        return trainerProfileService.getPublicProfile(trainerId);
    }

    @PreAuthorize("hasRole('TRAINER')")
    @GetMapping("/sessions")
    public PageResponse<TrainingSessionResponse> getMySessions(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        return trainingSessionService.getTrainerSessions(page,size);
    }
}
