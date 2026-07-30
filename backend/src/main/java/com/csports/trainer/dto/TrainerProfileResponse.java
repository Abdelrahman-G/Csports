package com.csports.trainer.dto;

public record TrainerProfileResponse(
        Long id,
        String name,
        String photoUrl,
        String bio,
        Integer experienceYears,
        Long sportId,
        String sport,
        Long regionId,
        String regionName,
        String city,
        String country
) {}
