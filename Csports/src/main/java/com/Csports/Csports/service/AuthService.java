package com.Csports.Csports.service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.Csports.Csports.DTO.LoginRequest;
import com.Csports.Csports.DTO.RegisterRequest;
import com.Csports.Csports.exception.EmailAlreadyExistsException;
import com.Csports.Csports.exception.InvalidCredentialsException;
import com.Csports.Csports.model.User;
import com.Csports.Csports.repository.UserRepository;


@Service 
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already exists");
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
    }
    public void login(LoginRequest request) {

        User user = userRepository.findByEmail(request.identifier())
            .or(() -> userRepository.findByPhoneNumber(request.identifier()))
            .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
    }
}