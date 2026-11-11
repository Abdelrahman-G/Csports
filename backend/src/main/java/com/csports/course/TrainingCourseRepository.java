package com.csports.course;

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

import com.csports.course.TrainingCourse;
import com.csports.user.User;

@Repository
public interface TrainingCourseRepository
        extends JpaRepository<TrainingCourse, Long>,
        JpaSpecificationExecutor<TrainingCourse> {

    @EntityGraph(attributePaths = {"trainer", "sport", "region"})
    Page<TrainingCourse> findByStatusAndStartDateGreaterThanEqual(
            TrainingCourseStatus status,
            LocalDate date,
            Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"trainer", "sport", "region"})
    Page<TrainingCourse> findAll(
            Specification<TrainingCourse> specification,
            Pageable pageable);

    Optional<TrainingCourse> findById(Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from TrainingCourse c where c.id = :id")
    Optional<TrainingCourse> findForAnnouncement(Long id);

    Page<TrainingCourse> findByTrainer(User trainer, Pageable pageable);

    List<TrainingCourse> findByStatusAndEndDateLessThanEqual(
            TrainingCourseStatus status,
            LocalDate endDate);

    @EntityGraph(attributePaths = {"trainer", "sport", "region"})
    @Query("select course from TrainingCourse course where course.id in :ids")
    List<TrainingCourse> findAllWithDetailsByIdIn(Collection<Long> ids);
}
