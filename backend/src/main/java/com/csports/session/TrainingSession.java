package com.csports.session;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.csports.booking.Booking;
import com.csports.location.Region;
import com.csports.sport.Sport;
import com.csports.user.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "training_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id", nullable = false)
    private User trainer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id", nullable = false)
    private Sport sport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    // Detects concurrent changes to the same session, including seat updates.
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private String locationName;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "training_session_days",
            joinColumns = @JoinColumn(name = "session_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "day", nullable = false)
    private Set<DayOfWeek> days = new HashSet<>();

    @Column(nullable = false)
    private Integer maxParticipants;

    @Builder.Default
    @Column(nullable = false)
    private Integer currentParticipants = 0;

    @Column(nullable = false)
    private Double price;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TrainingSessionStatus status = TrainingSessionStatus.SCHEDULED;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime cancelledAt;

    @Column(length = 500)
    private String lastUpdateReason;

    @Column(length = 500)
    private String cancellationReason;

    @OneToMany(mappedBy = "session",cascade = CascadeType.ALL,orphanRemoval = true)   
        @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    public String getGoogleMapsUrl() {
        return "https://www.google.com/maps?q=" +
                latitude + "," + longitude;
    }
}
