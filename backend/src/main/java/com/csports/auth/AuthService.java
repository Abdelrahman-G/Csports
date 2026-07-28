package com.csports.auth;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.csports.auth.dto.AuthResponse;
import com.csports.auth.dto.LoginRequest;
import com.csports.auth.dto.RefreshRequest;
import com.csports.auth.dto.RegisterRequest;
import com.csports.auth.dto.RegisterTrainerRequest;
import com.csports.auth.exception.EmailAlreadyExistsException;
import com.csports.auth.exception.InvalidCredentialsException;
import com.csports.auth.exception.PhoneNumberAlreadyExistsException;
import com.csports.common.exception.ResourceNotFoundException;
import com.csports.sport.exception.SportNotFoundException;
import com.csports.auth.RefreshToken;
import com.csports.location.Region;
import com.csports.location.UserLocation;
import com.csports.sport.Sport;
import com.csports.user.Role;
import com.csports.trainer.TrainerProfile;
import com.csports.user.User;
import com.csports.auth.RefreshTokenRepository;
import com.csports.location.RegionRepository;
import com.csports.sport.SportRepository;
import com.csports.trainer.TrainerProfileRepository;
import com.csports.user.UserRepository;
import com.csports.security.JwtService;
import com.csports.security.TokenBlacklistService;

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
        private final TokenBlacklistService tokenBlacklistService;

        public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                        RefreshTokenRepository refreshTokenRepository, SportRepository sportRepository,
                        TrainerProfileRepository trainerProfileRepository, RegionRepository regionRepository,
                        TokenBlacklistService tokenBlacklistService) {
                this.userRepository = userRepository;
                this.passwordEncoder = passwordEncoder;
                this.jwtService = jwtService;
                this.refreshTokenRepository = refreshTokenRepository;
                this.sportRepository = sportRepository;
                this.trainerProfileRepository = trainerProfileRepository;
                this.regionRepository = regionRepository;
                this.tokenBlacklistService = tokenBlacklistService;
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

        
        public void logout(String accessToken, RefreshRequest request) {

                RefreshToken refreshToken = refreshTokenRepository
                                .findByToken(request.refreshToken())
                                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

                refreshToken.setRevoked(true);
                tokenBlacklistService.blacklist(accessToken);
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
