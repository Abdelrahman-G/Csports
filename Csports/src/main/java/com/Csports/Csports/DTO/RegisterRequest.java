package com.Csports.Csports.DTO;

import com.Csports.Csports.model.Role;

public record RegisterRequest(
        String name,
        String email,
        String password,
        Integer age,
        String phoneNumber,
        Role role
) {}
