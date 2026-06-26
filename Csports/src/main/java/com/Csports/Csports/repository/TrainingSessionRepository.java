package com.Csports.Csports.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Csports.Csports.model.TrainingSession;
@Repository
public interface TrainingSessionRepository extends  JpaRepository<TrainingSession, Long> {
    
}
