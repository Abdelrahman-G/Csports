package com.Csports.Csports.model;

import java.util.HashSet;

import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "trainer_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerProfile {
    @Id
private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 1000)
    private String bio;

    private Integer experienceYears;

    @ManyToMany
    @JoinTable(
            name = "trainer_sports",
            joinColumns = @JoinColumn(name = "trainer_id"),
            inverseJoinColumns = @JoinColumn(name = "sport_id")
    )
    private final HashSet<Sport> sports = new HashSet<>();

    // private String location;
}
