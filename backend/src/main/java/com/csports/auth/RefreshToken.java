package com.csports.auth;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.csports.user.User;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;
    // tokens for multiple devices of one user
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean revoked;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
