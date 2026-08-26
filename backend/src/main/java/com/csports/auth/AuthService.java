package com.csports.auth;

import java.time.LocalDateTime;
import java.util.Locale;

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
import com.csports.sport.exception.SportNotFoundException;
import com.csports.sport.Sport;
import com.csports.user.Role;
import com.csports.trainer.TrainerProfile;
import com.csports.user.User;
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
        private final TokenBlacklistService tokenBlacklistService;

        public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                        RefreshTokenRepository refreshTokenRepository, SportRepository sportRepository,
                        TrainerProfileRepository trainerProfileRepository,
                        TokenBlacklistService tokenBlacklistService) {
                this.userRepository = userRepository;
                this.passwordEncoder = passwordEncoder;
                this.jwtService = jwtService;
                this.refreshTokenRepository = refreshTokenRepository;
                this.sportRepository = sportRepository;
                this.trainerProfileRepository = trainerProfileRepository;
                this.tokenBlacklistService = tokenBlacklistService;
        }

        @Transactional
        public void registerUser(RegisterRequest request) {

                String email = normalizeEmail(request.email());
                validateRegistration(email, request.phoneNumber());

                User user = User.builder()
                                .name(request.name())
                                .email(email)
                                .phoneNumber(request.phoneNumber())
                                .password(passwordEncoder.encode(request.password()))
                                .age(request.age())
                                .role(Role.USER)
                                .build();

                userRepository.save(user);
        }

        @Transactional
        public void registerTrainer(RegisterTrainerRequest request) {

                String email = normalizeEmail(request.email());
                validateRegistration(email, request.phoneNumber());

                Sport sport = sportRepository.findById(request.sportId())
                                .orElseThrow(SportNotFoundException::new);

                User user = User.builder()
                                .name(request.name())
                                .email(email)
                                .phoneNumber(request.phoneNumber())
                                .password(passwordEncoder.encode(request.password()))
                                .age(request.age())
                                .role(Role.TRAINER)
                                .build();

                userRepository.save(user);

                TrainerProfile trainerProfile = TrainerProfile.builder()
                                .user(user)
                                .bio(request.bio())
                                .experienceYears(request.experienceYears())
                                .sport(sport)
                                .build();

                trainerProfileRepository.save(trainerProfile);
        }

        @Transactional
        public AuthResponse login(LoginRequest request) {

                String identifier = request.identifier().trim();
                String normalizedEmail = normalizeEmail(identifier);
                User user = userRepository.findByEmail(normalizedEmail)
                                .or(() -> userRepository.findByPhoneNumber(identifier))
                                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

                if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                        throw new InvalidCredentialsException("Invalid credentials");
                }
                String accessToken = jwtService.generateAccessToken(user);
                String refreshToken = jwtService.generateRefreshToken(user);

                saveRefreshToken(refreshToken, user);

                return new AuthResponse(accessToken, refreshToken, user.getRole());
        }

        @Transactional
        public AuthResponse refresh(RefreshRequest request) {

                RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

                if (refreshToken.isRevoked()) {
                        throw new InvalidCredentialsException("Refresh token revoked");
                }

                User user = refreshToken.getUser();
                if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())
                                || !jwtService.isRefreshTokenValid(
                                                refreshToken.getToken(),
                                                user)) {
                        throw new InvalidCredentialsException("Refresh token is invalid or expired");
                }

                int consumedTokenCount = refreshTokenRepository.revokeIfActive(refreshToken.getId());
                if (consumedTokenCount != 1) {
                        throw new InvalidCredentialsException("Refresh token revoked");
                }

                // Keep the managed entity consistent with the atomic database update.
                refreshToken.setRevoked(true);
                String accessToken = jwtService.generateAccessToken(user);
                String rotatedRefreshToken = jwtService.generateRefreshToken(user);
                saveRefreshToken(rotatedRefreshToken, user);

                return new AuthResponse(accessToken, rotatedRefreshToken, user.getRole());
        }

        @Transactional
        public void logout(
                        String accessToken,
                        RefreshRequest request,
                        Long authenticatedUserId) {

                RefreshToken refreshToken = refreshTokenRepository
                                .findByToken(request.refreshToken())
                                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

                if (!refreshToken.getUser().getId().equals(authenticatedUserId)
                                || !jwtService.isRefreshTokenValid(
                                                refreshToken.getToken(),
                                                refreshToken.getUser())) {
                        throw new InvalidCredentialsException("Invalid refresh token");
                }

                refreshToken.setRevoked(true);
                tokenBlacklistService.blacklist(accessToken);
        }

        private void saveRefreshToken(String token, User user) {
                refreshTokenRepository.save(
                                RefreshToken.builder()
                                                .token(token)
                                                .user(user)
                                                .expiryDate(jwtService.getExpiration(token))
                                                .revoked(false)
                                                .build());
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

        private String normalizeEmail(String email) {
                return email.trim().toLowerCase(Locale.ROOT);
        }
}
