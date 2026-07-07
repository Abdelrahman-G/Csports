package com.Csports.Csports.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Csports.Csports.model.TrainingSession;
import com.Csports.Csports.model.User;

@Repository
public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {
    Page<TrainingSession> findByStartDateGreaterThanEqual(LocalDate date, Pageable pageable);

    Optional<TrainingSession> findById(Long id);

    Page<TrainingSession> findByTrainer(User trainer, Pageable pageable);

}
