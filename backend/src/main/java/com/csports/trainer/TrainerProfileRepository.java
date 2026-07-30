package com.csports.trainer;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.csports.trainer.TrainerProfile;
import com.csports.user.User;

@Repository
public interface TrainerProfileRepository extends JpaRepository<TrainerProfile, Long> {
    Optional<TrainerProfile> findByUser(User user);

    @Override
    @EntityGraph(attributePaths = {
            "user",
            "sport",
            "user.location",
            "user.location.region"
    })
    Optional<TrainerProfile> findById(Long id);
}
