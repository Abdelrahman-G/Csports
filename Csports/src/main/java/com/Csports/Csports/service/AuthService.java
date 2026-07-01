package com.Csports.Csports.service;
import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.Csports.Csports.DTO.AuthResponse;
import com.Csports.Csports.DTO.LoginRequest;
import com.Csports.Csports.DTO.RefreshRequest;
import com.Csports.Csports.DTO.RegisterRequest;
import com.Csports.Csports.exception.EmailAlreadyExistsException;
import com.Csports.Csports.exception.InvalidCredentialsException;
import com.Csports.Csports.exception.PhoneNumberAlreadyExistsException;
import com.Csports.Csports.model.RefreshToken;
import com.Csports.Csports.model.Sport;
import com.Csports.Csports.model.Role;
import com.Csports.Csports.model.TrainerProfile;
import com.Csports.Csports.model.User;
import com.Csports.Csports.repository.RefreshTokenRepository;
import com.Csports.Csports.repository.SportRepository;
import com.Csports.Csports.repository.TrainerProfileRepository;
import com.Csports.Csports.repository.UserRepository;
import com.Csports.Csports.security.JwtService;


@Service 
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SportRepository sportRepository;
    private final TrainerProfileRepository trainerProfileRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, RefreshTokenRepository refreshTokenRepository, SportRepository sportRepository, TrainerProfileRepository trainerProfileRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.sportRepository = sportRepository;
        this.trainerProfileRepository = trainerProfileRepository;
    }

    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new PhoneNumberAlreadyExistsException("this number is registered to and existing account");
        }
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .password(passwordEncoder.encode(request.password()))
                .age(request.age())
                .role(request.role())
                .build();

        userRepository.save(user);
        if (user.getRole() == Role.TRAINER) {

            Sport sport = sportRepository.findById(request.sportId())
            .orElseThrow(() -> new RuntimeException("Sport not found"));

            TrainerProfile profile = TrainerProfile.builder()
                .user(user)
                .bio(request.bio())
                .experienceYears(request.experienceYears())
                .sport(sport)
                .build();

            trainerProfileRepository.save(profile);
        }
    }
    public AuthResponse  login(LoginRequest request) {

        User user = userRepository.findByEmail(request.identifier())
            .or(() -> userRepository.findByPhoneNumber(request.identifier()))
            .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        refreshTokenRepository.save(
            RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .build()
        );

        return new AuthResponse(accessToken, refreshToken,user.getRole());
    }

    public AuthResponse refresh(RefreshRequest request) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new InvalidCredentialsException("Refresh token revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("Refresh token expired");
        }

        User user = refreshToken.getUser();

        String accessToken = jwtService.generateAccessToken(user);

        return new AuthResponse(accessToken, refreshToken.getToken(), user.getRole());
    }
    public void logout(RefreshRequest request) {

        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.refreshToken())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        refreshToken.setRevoked(true);

        refreshTokenRepository.save(refreshToken);
    }


}