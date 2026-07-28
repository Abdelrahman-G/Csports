package com.csports.auth.dto;


public record RegisterRequest(

        String name,

        String email,

        String phoneNumber,

        String password,

        Integer age,

        
        Long regionId,

        Double latitude,

        Double longitude

) {}