package com.Csports.Csports.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Csports.Csports.model.Booking;
import com.Csports.Csports.model.TrainingSession;
import com.Csports.Csports.model.User;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByUserAndSession(User user, TrainingSession session);

    List<Booking> findByUser(User user);
}