package com.Csports.Csports.DTO;
public record LoginRequest(
        String identifier,
        String password
) {}