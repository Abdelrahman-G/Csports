package com.csports.location.dto;

public record ResolvedGoogleMapsLocationResponse(
        double latitude,
        double longitude,
        String normalizedGoogleMapsUrl) {
}
