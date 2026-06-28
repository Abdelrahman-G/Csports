package com.Csports.Csports.DTO;

import com.Csports.Csports.model.Role;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Role role

) {}