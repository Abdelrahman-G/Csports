package com.csports.trainer.dto;



public record CreateTrainerProfileRequest(

        String bio,

        Integer experienceYears,

        Long sportId

) {}