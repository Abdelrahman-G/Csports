package com.csports.location;

import com.csports.user.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLocation {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false, unique = true)
        private User user;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "region_id", nullable = false)
        private Region region;

        @Column(nullable = false)
        private Double latitude;

        @Column(nullable = false)
        private Double longitude;
}
