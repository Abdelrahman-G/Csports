package com.Csports.Csports.DTO;

public record RegisterTrainerRequest(
        String name,
        String email,
        String phoneNumber,
        String password,
        Integer age,
        String bio,
        Integer experienceYears,
        Long sportId,
        Long regionId,
        Double latitude,
        Double longitude
){}
