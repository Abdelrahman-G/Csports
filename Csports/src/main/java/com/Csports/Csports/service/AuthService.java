package com.Csports.Csports.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.Csports.Csports.DTO.AuthResponse;
import com.Csports.Csports.DTO.LoginRequest;
import com.Csports.Csports.DTO.RefreshRequest;
import com.Csports.Csports.DTO.RegisterRequest;
import com.Csports.Csports.DTO.RegisterTrainerRequest;
import com.Csports.Csports.exception.EmailAlreadyExistsException;
import com.Csports.Csports.exception.InvalidCredentialsException;
import com.Csports.Csports.exception.PhoneNumberAlreadyExistsException;
import com.Csports.Csports.exception.ResourceNotFoundException;
import com.Csports.Csports.exception.SportNotFoundException;
import com.Csports.Csports.model.RefreshToken;
import com.Csports.Csports.model.Region;
import com.Csports.Csports.model.Sport;
import com.Csports.Csports.model.Role;
import com.Csports.Csports.model.TrainerProfile;
import com.Csports.Csports.model.User;
import com.Csports.Csports.model.UserLocation;
import com.Csports.Csports.repository.RefreshTokenRepository;
import com.Csports.Csports.repository.RegionRepository;
import com.Csports.Csports.repository.SportRepository;
import com.Csports.Csports.repository.TrainerProfileRepository;
import com.Csports.Csports.repository.UserRepository;
import com.Csports.Csports.security.JwtService;

import jakarta.transaction.Transactional;

@Service
public class AuthService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final RefreshTokenRepository refreshTokenRepository;
        private final SportRepository sportRepository;
        private final TrainerProfileRepository trainerProfileRepository;
        private final RegionRepository regionRepository;

        public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                        RefreshTokenRepository refreshTokenRepository, SportRepository sportRepository,
                        TrainerProfileRepository trainerProfileRepository, RegionRepository regionRepository) {
                this.userRepository = userRepository;
                this.passwordEncoder = passwordEncoder;
                this.jwtService = jwtService;
                this.refreshTokenRepository = refreshTokenRepository;
                this.sportRepository = sportRepository;
                this.trainerProfileRepository = trainerProfileRepository;
                this.regionRepository = regionRepository;
        }

        @Transactional
        public void registerUser(RegisterRequest request) {

                validateRegistration(request.email(), request.phoneNumber());

                Region region = regionRepository.findById(request.regionId())
                                .orElseThrow(() -> new ResourceNotFoundException("Region not found."));

                User user = User.builder()
                                .name(request.name())
                                .email(request.email())
                                .phoneNumber(request.phoneNumber())
                                .password(passwordEncoder.encode(request.password()))
                                .age(request.age())
                                .role(Role.USER)
                                .build();

                UserLocation location = UserLocation.builder()
                                .region(region)
                                .latitude(request.latitude())
                                .longitude(request.longitude())
                                .build();

                user.setLocation(location);

                userRepository.save(user);
        }

        @Transactional
        public void registerTrainer(RegisterTrainerRequest request) {

                validateRegistration(request.email(), request.phoneNumber());

                Region region = regionRepository.findById(request.regionId())
                                .orElseThrow(() -> new ResourceNotFoundException("Region not found."));

                Sport sport = sportRepository.findById(request.sportId())
                                .orElseThrow(SportNotFoundException::new);

                User user = User.builder()
                                .name(request.name())
                                .email(request.email())
                                .phoneNumber(request.phoneNumber())
                                .password(passwordEncoder.encode(request.password()))
                                .age(request.age())
                                .role(Role.TRAINER)
                                .build();

                UserLocation location = UserLocation.builder()
                                .region(region)
                                .latitude(request.latitude())
                                .longitude(request.longitude())
                                .build();

                user.setLocation(location);

                userRepository.save(user);

                TrainerProfile trainerProfile = TrainerProfile.builder()
                                .user(user)
                                .bio(request.bio())
                                .experienceYears(request.experienceYears())
                                .sport(sport)
                                .build();

                trainerProfileRepository.save(trainerProfile);
        }

        public AuthResponse login(LoginRequest request) {

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
                                                .build());

                return new AuthResponse(accessToken, refreshToken, user.getRole());
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

        private void validateRegistration(String email, String phoneNumber) {

                if (userRepository.existsByEmail(email)) {
                        throw new EmailAlreadyExistsException("Email already exists.");
                }

                if (userRepository.existsByPhoneNumber(phoneNumber)) {
                        throw new PhoneNumberAlreadyExistsException(
                                        "This phone number is already registered.");
                }
        }
}