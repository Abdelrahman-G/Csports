package com.csports.location;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "region",indexes = {@Index(name = "idx_region_name", columnList = "name")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false, unique = true)
    private String name;

    // Center point of the region
    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;
}