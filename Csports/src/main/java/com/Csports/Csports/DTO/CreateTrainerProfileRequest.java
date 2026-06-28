package com.Csports.Csports.DTO;

import java.util.Set;

public record CreateTrainerProfileRequest(

        String bio,

        Integer experienceYears,

        Set<Long> sportIds

) {}