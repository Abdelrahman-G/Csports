package com.Csports.Csports.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.Builder;
@Entity
@Builder
public class Booking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TrainingSession session;

    // @Enumerated(EnumType.STRING)
    // @Column(nullable = false)
    // private BookingStatus status;

    @CreationTimestamp
    private LocalDateTime bookedAt;

    // versioning the booking to avoid concurrent updates (optimistic locking)
    @Version
    private Long version;   
}
