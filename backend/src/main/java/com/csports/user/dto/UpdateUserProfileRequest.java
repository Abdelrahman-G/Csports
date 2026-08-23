package com.csports.user.dto;

import com.csports.common.validation.EgyptianPhoneNumber;
import com.csports.common.validation.ServiceArea;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * A partial account update. Null fields are omitted and retain their current
 * value. Password and role changes intentionally use neither this DTO nor this
 * endpoint.
 */
public record UpdateUserProfileRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "Name must not be blank")
        @Size(min = 2, max = 100, message = "Name must contain between 2 and 100 characters")
        String name,

        @Pattern(regexp = "(?s).*\\S.*", message = "Email must not be blank")
        @Email(message = "Email must have a valid format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Pattern(regexp = EgyptianPhoneNumber.REGEX, message = EgyptianPhoneNumber.MESSAGE)
        String phoneNumber,

        @Min(value = 13, message = "Age must be at least 13")
        @Max(value = 100, message = "Age must not exceed 100")
        Integer age,

        @Positive(message = "Region id must be positive")
        Long regionId,

        @DecimalMin(
                value = ServiceArea.MIN_LATITUDE,
                message = "Latitude must be within the Cairo and Giza service area")
        @DecimalMax(
                value = ServiceArea.MAX_LATITUDE,
                message = "Latitude must be within the Cairo and Giza service area")
        Double latitude,

        @DecimalMin(
                value = ServiceArea.MIN_LONGITUDE,
                message = "Longitude must be within the Cairo and Giza service area")
        @DecimalMax(
                value = ServiceArea.MAX_LONGITUDE,
                message = "Longitude must be within the Cairo and Giza service area")
        Double longitude
) {
    public boolean hasEditableFields() {
        return name != null
                || email != null
                || phoneNumber != null
                || age != null
                || regionId != null
                || latitude != null
                || longitude != null;
    }

    public boolean hasLocationFields() {
        return regionId != null || latitude != null || longitude != null;
    }
}
