package com.csports.booking;

import java.util.Optional;
import java.util.List;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.csports.course.TrainingCourse;
import com.csports.user.User;

@Repository
public interface BookingRepository
        extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    boolean existsByUserAndCourseAndStatus(
            User user,
            TrainingCourse course,
            BookingStatus status);

    Optional<Booking> findByUserAndCourseAndStatus(
            User user,
            TrainingCourse course,
            BookingStatus status);

    Page<Booking> findByUserAndStatus(
            User user,
            BookingStatus status,
            Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {
            "course",
            "course.trainer",
            "course.sport",
            "course.region"
    })
    Page<Booking> findAll(Specification<Booking> specification, Pageable pageable);

    Page<Booking> findByCourseAndStatus(
            TrainingCourse course,
            BookingStatus status,
            Pageable pageable);

    List<Booking> findAllByCourseAndStatus(
            TrainingCourse course,
            BookingStatus status);

    // shortcut: loads historical candidates; add an interval prefilter if large booking histories make this slow.
    @Query("""
            select distinct b from Booking b
            join fetch b.course s
            join fetch s.sport
            join fetch s.days
            where b.user.id = :userId and s.startDate < :to
            """)
    List<Booking> findScheduleCandidates(@Param("userId") Long userId, @Param("to") LocalDate to);

    @Query("""
            select b.user.id
            from Booking b
            where b.course.id = :courseId
              and b.status = com.csports.booking.BookingStatus.CONFIRMED
            """)
    List<Long> findBookedUserIdsByCourseId(@Param("courseId") Long courseId);
}
