package com.csports.booking;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.csports.session.TrainingSession;
import com.csports.user.User;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByUserAndSessionAndStatus(
            User user,
            TrainingSession session,
            BookingStatus status);

    Optional<Booking> findByUserAndSessionAndStatus(
            User user,
            TrainingSession session,
            BookingStatus status);

    Page<Booking> findByUserAndStatus(
            User user,
            BookingStatus status,
            Pageable pageable);

    Page<Booking> findBySessionAndStatus(
            TrainingSession session,
            BookingStatus status,
            Pageable pageable);

    List<Booking> findAllBySessionAndStatus(
            TrainingSession session,
            BookingStatus status);

    @Query("""
            select b.user.id
            from Booking b
            where b.session.id = :sessionId
              and b.status = com.csports.booking.BookingStatus.CONFIRMED
            """)
    List<Long> findBookedUserIdsBySessionId(@Param("sessionId") Long sessionId);
}
