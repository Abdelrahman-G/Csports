package com.Csports.Csports.DTO;

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

        Long sportId,
        
        Long regionId,

        Double latitude,

        Double longitude

) {}