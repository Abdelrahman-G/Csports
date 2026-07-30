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

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BookingStatus status = BookingStatus.CONFIRMED;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime bookedAt;

    private LocalDateTime cancelledAt;

    /**
     * A nullable marker used by the database's unique index. Confirmed
     * bookings use 1; historical bookings use NULL, allowing a user to book
     * the same session again after cancelling while still preventing two
     * simultaneously confirmed bookings.
     */
    @Column(name = "active_marker")
    private Short activeMarker;

    // versioning the booking to avoid concurrent updates (optimistic locking)
    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    @PreUpdate
    void synchronizeActiveMarker() {
        activeMarker = status == BookingStatus.CONFIRMED ? (short) 1 : null;
    }
}
