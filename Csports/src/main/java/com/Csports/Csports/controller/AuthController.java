package com.Csports.Csports.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Csports.Csports.DTO.LoginRequest;
import com.Csports.Csports.DTO.RegisterRequest;
import com.Csports.Csports.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {
        private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {

        authService.register(request);

        return ResponseEntity.ok("User registered successfully");
    }
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {

        authService.login(request);

        return ResponseEntity.ok("Login successful");
    }
}
