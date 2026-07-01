package com.Csports.Csports.DTO;



public record CreateTrainerProfileRequest(

        String bio,

        Integer experienceYears,

        Long sportId

) {}