package com.csports.common.exception;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        int status,
        String code,
        String error,
        String message,
        String path,
        Instant timestamp,
        Map<String, String> fieldErrors
) {
    public ErrorResponse {
        fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors);
    }

    public static ErrorResponse of(
            int status,
            String code,
            String error,
            String message,
            String path) {
        return new ErrorResponse(status, code, error, message, path, Instant.now(), Map.of());
    }
}
