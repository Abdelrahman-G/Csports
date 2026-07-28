package com.csports.booking;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.csports.booking.Booking;
import com.csports.session.TrainingSession;
import com.csports.user.User;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByUserAndSession(User user, TrainingSession session);
    Optional<Booking> findByUserAndSession(User user, TrainingSession session);
    Page<Booking> findByUser(User user, Pageable pageable);
    Page<Booking> findBySession(TrainingSession session, Pageable pageable);

}