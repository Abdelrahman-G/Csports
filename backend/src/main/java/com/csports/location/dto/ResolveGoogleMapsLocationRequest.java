package com.csports.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveGoogleMapsLocationRequest(
        @NotBlank(message = "Google Maps link is required")
        @Size(max = 2048, message = "Google Maps link is too long")
        String url) {
}
