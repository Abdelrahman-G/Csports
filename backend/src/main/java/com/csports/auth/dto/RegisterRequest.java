package com.csports.auth.dto;

import com.csports.common.validation.ServiceArea;
import com.csports.common.validation.EgyptianPhoneNumber;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must contain between 2 and 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must have a valid format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = EgyptianPhoneNumber.REGEX, message = EgyptianPhoneNumber.MESSAGE)
        String phoneNumber,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must contain between 8 and 72 characters")
        String password,

        @NotNull(message = "Age is required")
        @Min(value = 13, message = "Age must be at least 13")
        @Max(value = 100, message = "Age must not exceed 100")
        Integer age,

        @NotNull(message = "Region is required")
        @Positive(message = "Region id must be positive")
        Long regionId,

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = ServiceArea.MIN_LATITUDE, message = "Latitude must be within the Cairo and Giza service area")
        @DecimalMax(value = ServiceArea.MAX_LATITUDE, message = "Latitude must be within the Cairo and Giza service area")
        Double latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = ServiceArea.MIN_LONGITUDE, message = "Longitude must be within the Cairo and Giza service area")
        @DecimalMax(value = ServiceArea.MAX_LONGITUDE, message = "Longitude must be within the Cairo and Giza service area")
        Double longitude

) {}
