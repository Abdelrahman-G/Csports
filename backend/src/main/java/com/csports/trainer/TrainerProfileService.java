package com.csports.trainer;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csports.common.exception.ResourceNotFoundException;
import com.csports.location.Region;
import com.csports.location.UserLocation;
import com.csports.trainer.dto.CreateTrainerProfileRequest;
import com.csports.trainer.dto.TrainerProfileResponse;
import com.csports.trainer.dto.UpdateTrainerProfileRequest;
import com.csports.auth.exception.InvalidCredentialsException;
import com.csports.sport.exception.SportNotFoundException;
import com.csports.trainer.exception.TrainerProfileAlreadyExistsException;
import com.csports.trainer.exception.TrainerProfileNotFoundException;
import com.csports.user.Role;
import com.csports.sport.Sport;
import com.csports.trainer.TrainerProfile;
import com.csports.user.User;
import com.csports.sport.SportRepository;
import com.csports.trainer.TrainerProfileRepository;
import com.csports.user.UserService;
import com.csports.user.exception.InvalidProfileUpdateException;


@Service
public class TrainerProfileService {

    private final TrainerProfileRepository trainerProfileRepository;
    private final SportRepository sportRepository;
    private final UserService userService;

    public TrainerProfileService(
            TrainerProfileRepository trainerProfileRepository,
            SportRepository sportRepository,
            UserService userService) {

        this.trainerProfileRepository = trainerProfileRepository;
        this.sportRepository = sportRepository;
        this.userService = userService;
    }

    public void createProfile(CreateTrainerProfileRequest request) {

        User user = userService.getCurrentUser();

        if (user.getRole() != Role.TRAINER) {
            throw new InvalidCredentialsException("Only trainers can create a trainer profile.");
        }

        if (trainerProfileRepository.findByUser(user).isPresent()) {
            throw new TrainerProfileAlreadyExistsException();
        }

        Sport sport = sportRepository.findById(request.sportId())
                .orElseThrow(SportNotFoundException::new);

        TrainerProfile profile = TrainerProfile.builder()
                .user(user)
                .bio(request.bio())
                .experienceYears(request.experienceYears())
                .sport(sport)
                .build();

        trainerProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public TrainerProfileResponse getMyProfile() {
        User currentUser = userService.getCurrentUser();
        return toResponse(trainerProfileRepository.findById(currentUser.getId())
                .orElseThrow(TrainerProfileNotFoundException::new));
    }

    @Transactional(readOnly = true)
    public TrainerProfileResponse getPublicProfile(Long trainerId) {
        TrainerProfile profile = trainerProfileRepository.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trainer profile not found."));
        return toResponse(profile);
    }

    @Transactional
    public TrainerProfileResponse updateMyProfile(
            UpdateTrainerProfileRequest request) {
        User currentUser = userService.getCurrentUser();
        TrainerProfile profile = trainerProfileRepository.findById(currentUser.getId())
                .orElseThrow(TrainerProfileNotFoundException::new);

        if (!request.hasEditableFields()) {
            throw new InvalidProfileUpdateException(
                    "At least one editable trainer profile field must be provided.");
        }
        if (request.bio() != null) {
            profile.setBio(request.bio().isBlank() ? null : request.bio().trim());
        }
        if (request.experienceYears() != null) {
            profile.setExperienceYears(request.experienceYears());
        }

        return toResponse(profile);
    }

    private TrainerProfileResponse toResponse(TrainerProfile profile) {
        User user = profile.getUser();
        UserLocation location = user.getLocation();
        Region region = location == null ? null : location.getRegion();
        return new TrainerProfileResponse(
                user.getId(),
                user.getName(),
                user.getPhotoUrl(),
                profile.getBio(),
                profile.getExperienceYears(),
                profile.getSport().getId(),
                profile.getSport().getName(),
                region == null ? null : region.getId(),
                region == null ? null : region.getName(),
                region == null ? null : region.getCity(),
                region == null ? null : region.getCountry());
    }
}
