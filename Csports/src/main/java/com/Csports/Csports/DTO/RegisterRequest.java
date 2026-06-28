package com.Csports.Csports.DTO;

import java.util.Set;

import com.Csports.Csports.model.Role;

public record RegisterRequest(

        String name,

        String email,

        String phoneNumber,

        String password,

        Integer age,

        Role role,

        String bio,

        Integer experienceYears,

        Set<Long> sportIds

) {}