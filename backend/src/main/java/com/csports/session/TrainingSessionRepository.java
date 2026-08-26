package com.csports.session;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.csports.session.TrainingSession;
import com.csports.user.User;

@Repository
public interface TrainingSessionRepository
        extends JpaRepository<TrainingSession, Long>,
        JpaSpecificationExecutor<TrainingSession> {

    @EntityGraph(attributePaths = {"trainer", "sport", "region"})
    Page<TrainingSession> findByStatusAndStartDateGreaterThanEqual(
            TrainingSessionStatus status,
            LocalDate date,
            Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"trainer", "sport", "region"})
    Page<TrainingSession> findAll(
            Specification<TrainingSession> specification,
            Pageable pageable);

    Optional<TrainingSession> findById(Long id);

    Page<TrainingSession> findByTrainer(User trainer, Pageable pageable);

    List<TrainingSession> findByStatusAndEndDateLessThanEqual(
            TrainingSessionStatus status,
            LocalDate endDate);

    @EntityGraph(attributePaths = {"trainer", "sport", "region"})
    @Query("select session from TrainingSession session where session.id in :ids")
    List<TrainingSession> findAllWithDetailsByIdIn(Collection<Long> ids);
}
