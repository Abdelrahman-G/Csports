package com.csports.user.dto;

import com.csports.user.Role;

public record UserProfileResponse(
        Long id,
        String name,
        String email,
        String phoneNumber,
        Integer age,
        Role role,
        String photoUrl,
        Long regionId,
        String regionName,
        String city,
        String country,
        Double latitude,
        Double longitude
) {}
