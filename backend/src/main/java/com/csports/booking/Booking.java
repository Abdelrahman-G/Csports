package com.csports.booking;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.csports.session.TrainingSession;
import com.csports.user.User;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
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
