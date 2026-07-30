package com.csports.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.csports.common.web.ApiPaths;
import com.csports.user.User;
import com.csports.user.UserService;
import com.csports.user.dto.UserProfileResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping({ApiPaths.AUTH, ApiPaths.LEGACY_AUTH})
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    /**
     * Backward-compatible alias. New clients should use /api/v1/users/me.
     */
    @Deprecated
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public UserProfileResponse me() {
        return userService.getMyProfile();
    }

    @PostMapping("/register/user")
    public ResponseEntity<String> registerUser(@Valid @RequestBody RegisterRequest request) {

        authService.registerUser(request);

        return ResponseEntity.ok("User registered successfully.");
    }

    @PostMapping("/register/trainer")
    public ResponseEntity<String> registerTrainer(
            @Valid @RequestBody RegisterTrainerRequest request) {

        authService.registerTrainer(request);

        return ResponseEntity.ok("Trainer registered successfully.");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse authResponse = authService.refresh(request);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @Valid @RequestBody RefreshRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest,
            Authentication authentication) {
        String authHeader = httpRequest.getHeader("Authorization");
        String accessToken = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;
        User currentUser = (User) authentication.getPrincipal();
        authService.logout(accessToken, request, currentUser.getId());
        
        return ResponseEntity.ok("Logged out successfully");
    }
}
