package com.csports.course.dto;

public record CourseParticipantResponse(

        Long userId,

        String name,

        String email,

        String phoneNumber

) {}