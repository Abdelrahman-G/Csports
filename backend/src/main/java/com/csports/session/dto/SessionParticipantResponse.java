package com.csports.session.dto;

public record SessionParticipantResponse(

        Long userId,

        String name,

        String email,

        String phoneNumber

) {}