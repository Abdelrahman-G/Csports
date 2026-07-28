package com.csports.trainer;


import org.springframework.stereotype.Service;
import com.csports.trainer.dto.CreateTrainerProfileRequest;
import com.csports.auth.exception.InvalidCredentialsException;
import com.csports.sport.exception.SportNotFoundException;
import com.csports.trainer.exception.TrainerProfileAlreadyExistsException;
import com.csports.user.Role;
import com.csports.sport.Sport;
import com.csports.trainer.TrainerProfile;
import com.csports.user.User;
import com.csports.sport.SportRepository;
import com.csports.trainer.TrainerProfileRepository;
import com.csports.user.UserService;


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
