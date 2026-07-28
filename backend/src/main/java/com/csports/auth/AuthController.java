package com.csports.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.csports.auth.dto.AuthResponse;
import com.csports.auth.dto.LoginRequest;
import com.csports.auth.dto.RefreshRequest;
import com.csports.auth.dto.RegisterRequest;
import com.csports.auth.dto.RegisterTrainerRequest;
import com.csports.auth.AuthService;
import com.csports.common.web.ApiPaths;

@RestController
@RequestMapping({ApiPaths.AUTH, ApiPaths.LEGACY_AUTH})
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
    public ResponseEntity<String> logout(@RequestBody RefreshRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        String accessToken = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;
        authService.logout(accessToken, request);
        
        return ResponseEntity.ok("Logged out successfully");
    }
}
