package com.Csports.Csports.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Csports.Csports.DTO.AuthResponse;
import com.Csports.Csports.DTO.LoginRequest;
import com.Csports.Csports.DTO.RefreshRequest;
import com.Csports.Csports.DTO.RegisterRequest;
import com.Csports.Csports.DTO.RegisterTrainerRequest;
import com.Csports.Csports.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
@GetMapping("/me")
public String me(Authentication authentication) {
    return authentication.getAuthorities().toString();
}
    @PostMapping("/register/user")
    public ResponseEntity<String> registerUser(@RequestBody RegisterRequest request) {

        authService.registerUser(request);

        return ResponseEntity.ok("User registered successfully.");
    }

    @PostMapping("/register/trainer")
    public ResponseEntity<String> registerTrainer(
            @RequestBody RegisterTrainerRequest request) {

        authService.registerTrainer(request);

        return ResponseEntity.ok("Trainer registered successfully.");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        AuthResponse authResponse = authService.refresh(request);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody RefreshRequest request) {
        authService.logout(request);
        
        return ResponseEntity.ok("Logged out successfully");
    }
}
