package com.Csports.Csports.service;


import org.springframework.stereotype.Service;
import com.Csports.Csports.DTO.CreateTrainerProfileRequest;
import com.Csports.Csports.exception.InvalidCredentialsException;
import com.Csports.Csports.exception.SportNotFoundException;
import com.Csports.Csports.exception.TrainerProfileAlreadyExistsException;
import com.Csports.Csports.model.Role;
import com.Csports.Csports.model.Sport;
import com.Csports.Csports.model.TrainerProfile;
import com.Csports.Csports.model.User;
import com.Csports.Csports.repository.SportRepository;
import com.Csports.Csports.repository.TrainerProfileRepository;


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

    
}