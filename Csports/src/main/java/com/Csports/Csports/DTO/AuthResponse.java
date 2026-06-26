package com.Csports.Csports.DTO;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {}