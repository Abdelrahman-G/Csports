package com.csports.auth.dto;

import com.csports.user.Role;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Role role

) {}