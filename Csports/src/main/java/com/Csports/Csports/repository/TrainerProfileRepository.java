package com.Csports.Csports.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Csports.Csports.model.TrainerProfile;

@Repository
public interface TrainerProfileRepository extends JpaRepository<TrainerProfile, Long> {
}