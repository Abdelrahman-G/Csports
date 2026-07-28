package com.csports.auth.dto;
public record LoginRequest(
        String identifier,
        String password
) {}