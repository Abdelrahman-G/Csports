package com.Csports.Csports.DTO;

public record SessionParticipantResponse(

        Long userId,

        String name,

        String email,

        String phoneNumber

) {}