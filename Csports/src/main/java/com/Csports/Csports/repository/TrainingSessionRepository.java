package com.Csports.Csports.repository;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Csports.Csports.model.TrainingSession;
@Repository
public interface TrainingSessionRepository extends  JpaRepository<TrainingSession, Long> {
    Page<TrainingSession> findByStartDateGreaterThanEqual(LocalDate date,org.springframework.data.domain.Pageable pageable);
    Optional<TrainingSession> findById(Long id);

}
