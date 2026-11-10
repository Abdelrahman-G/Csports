package com.csports.trainer;

import com.csports.common.web.ApiPaths;
import com.csports.common.pagination.PageResponse;
import com.csports.course.dto.TrainingCourseResponse;
import com.csports.course.TrainingCourseService;
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

    private final TrainingCourseService trainingCourseService;
    private final TrainerProfileService trainerProfileService;

    public TrainerController(
            TrainingCourseService trainingCourseService,
            TrainerProfileService trainerProfileService) {
        this.trainingCourseService = trainingCourseService;
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
    @GetMapping("/courses")
    public PageResponse<TrainingCourseResponse> getMyCourses(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        return trainingCourseService.getTrainerCourses(page,size);
    }
}
