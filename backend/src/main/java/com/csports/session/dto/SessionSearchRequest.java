package com.csports.session.dto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.format.annotation.DateTimeFormat;

import com.csports.session.exception.InvalidSessionSearchException;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Public query parameters for session discovery.
 *
 * Null optional filters are intentionally omitted from the database predicate.
 * Pagination and sorting defaults are normalized here so equivalent HTTP
 * requests share one Redis key.
 */
public record SessionSearchRequest(
        @Size(max = 100, message = "Search text must not exceed 100 characters")
        String q,

        @Positive(message = "Sport id must be positive")
        Long sportId,

        @Positive(message = "Trainer id must be positive")
        Long trainerId,

        @Positive(message = "Region id must be positive")
        Long regionId,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fromDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate toDate,

        @DecimalMin(value = "0.0", message = "Minimum price cannot be negative")
        Double minPrice,

        @DecimalMin(value = "0.0", message = "Maximum price cannot be negative")
        Double maxPrice,

        Boolean availableOnly,

        @Pattern(
                regexp = "(?i)^(startDate|price|createdAt)$",
                message = "Sort field must be startDate, price, or createdAt")
        String sortBy,

        @Pattern(
                regexp = "(?i)^(asc|desc)$",
                message = "Sort direction must be asc or desc")
        String direction,

        @Min(value = 0, message = "Page must be zero or greater")
        Integer page,

        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size must not exceed 100")
        Integer size
) {
    public SessionSearchRequest {
        q = normalizeQuery(q);
        availableOnly = availableOnly != null && availableOnly;
        sortBy = sortBy == null || sortBy.isBlank() ? "startDate" : sortBy;
        direction = direction == null || direction.isBlank()
                ? "asc"
                : direction.toLowerCase(Locale.ROOT);
        page = page == null ? 0 : page;
        size = size == null ? 10 : size;
    }

    public LocalDate effectiveFromDate() {
        return fromDate == null ? LocalDate.now() : fromDate;
    }

    public void validateRanges() {
        if (toDate != null && toDate.isBefore(effectiveFromDate())) {
            throw new InvalidSessionSearchException(
                    "The end of the search date range cannot be before its start.");
        }
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new InvalidSessionSearchException(
                    "Maximum price cannot be lower than minimum price.");
        }
    }

    /**
     * Hashing keeps Redis keys short and avoids placing raw user search text in
     * infrastructure keys.
     */
    public String cacheKey() {
        validateRanges();
        String canonical = String.join(
                "|",
                "v1",
                value(q),
                value(sportId),
                value(trainerId),
                value(regionId),
                value(effectiveFromDate()),
                value(toDate),
                value(minPrice),
                value(maxPrice),
                value(availableOnly),
                sortBy.toLowerCase(Locale.ROOT),
                direction,
                value(page),
                value(size));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }

    private static String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.strip().toLowerCase(Locale.ROOT);
    }

    private static String value(Object value) {
        return value == null ? "" : value.toString();
    }
}
