package com.csports.location;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.csports.common.validation.ServiceArea;
import com.csports.location.dto.ResolvedGoogleMapsLocationResponse;
import com.csports.location.exception.InvalidGoogleMapsLocationException;

@Service
public class GoogleMapsLocationService {

    private static final int MAX_REDIRECTS = 5;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final Pattern AT_COORDINATES = Pattern.compile(
            "@(-?\\d{1,3}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)");
    private static final Pattern DATA_COORDINATES = Pattern.compile(
            "!3d(-?\\d{1,3}(?:\\.\\d+)?)!4d(-?\\d{1,3}(?:\\.\\d+)?)");
    private static final Pattern PLAIN_COORDINATES = Pattern.compile(
            "^\\s*(-?\\d{1,3}(?:\\.\\d+)?)\\s*,\\s*(-?\\d{1,3}(?:\\.\\d+)?)\\s*$");
    private static final List<String> COORDINATE_QUERY_PARAMETERS = List.of(
            "q", "query", "ll", "destination", "center");

    private final HttpClient httpClient;

    public GoogleMapsLocationService() {
        this(HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    GoogleMapsLocationService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public ResolvedGoogleMapsLocationResponse resolve(String suppliedUrl) {
        URI locationUri = parseAndValidateUri(suppliedUrl);
        if (isGoogleShortLink(locationUri.getHost())) {
            locationUri = followGoogleRedirects(locationUri);
        }

        Coordinates coordinates = extractCoordinates(locationUri);
        validateServiceArea(coordinates);
        return new ResolvedGoogleMapsLocationResponse(
                coordinates.latitude(),
                coordinates.longitude(),
                normalizedUrl(coordinates));
    }

    private URI followGoogleRedirects(URI uri) {
        URI current = uri;
        try {
            for (int redirect = 0; redirect < MAX_REDIRECTS; redirect++) {
                HttpRequest request = HttpRequest.newBuilder(current)
                        .timeout(REQUEST_TIMEOUT)
                        .header("User-Agent", "Csports/1.0")
                        .GET()
                        .build();
                HttpResponse<Void> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.discarding());

                if (response.statusCode() < 300 || response.statusCode() >= 400) {
                    return current;
                }

                String location = response.headers().firstValue("Location")
                        .orElseThrow(() -> invalid("The shortened Google Maps link has no destination."));
                current = parseAndValidateUri(current.resolve(location).toString());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw invalid("Google Maps link verification was interrupted. Please try again.");
        } catch (IOException exception) {
            throw invalid("The shortened Google Maps link could not be opened. Please try again.");
        }
        throw invalid("The shortened Google Maps link redirects too many times.");
    }

    private URI parseAndValidateUri(String value) {
        try {
            URI uri = URI.create(value.trim());
            String host = uri.getHost();
            boolean approvedHost = host != null
                    && (host.equalsIgnoreCase("google.com")
                    || host.toLowerCase(Locale.ROOT).endsWith(".google.com")
                    || isGoogleShortLink(host));
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !approvedHost
                    || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)) {
                throw invalid("Enter a valid HTTPS Google Maps link.");
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw invalid("Enter a valid HTTPS Google Maps link.");
        }
    }

    private boolean isGoogleShortLink(String host) {
        return host.equalsIgnoreCase("maps.app.goo.gl") || host.equalsIgnoreCase("goo.gl");
    }

    private Coordinates extractCoordinates(URI uri) {
        String decodedUrl = URLDecoder.decode(uri.toString(), StandardCharsets.UTF_8);
        Coordinates coordinates = matchCoordinates(AT_COORDINATES, decodedUrl);
        if (coordinates == null) {
            coordinates = matchCoordinates(DATA_COORDINATES, decodedUrl);
        }
        if (coordinates == null && uri.getRawQuery() != null) {
            coordinates = coordinatesFromQuery(uri.getRawQuery());
        }
        if (coordinates == null) {
            throw invalid("The Google Maps link does not contain a readable location. Copy a place or dropped-pin link from Google Maps.");
        }
        return coordinates;
    }

    private Coordinates coordinatesFromQuery(String rawQuery) {
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length != 2 || !COORDINATE_QUERY_PARAMETERS.contains(
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8))) {
                continue;
            }
            Coordinates coordinates = matchCoordinates(
                    PLAIN_COORDINATES,
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            if (coordinates != null) {
                return coordinates;
            }
        }
        return null;
    }

    private Coordinates matchCoordinates(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        if (!matcher.find()) {
            return null;
        }
        try {
            return new Coordinates(
                    Double.parseDouble(matcher.group(1)),
                    Double.parseDouble(matcher.group(2)));
        } catch (NumberFormatException exception) {
            throw invalid("The Google Maps link contains invalid coordinates.");
        }
    }

    private void validateServiceArea(Coordinates coordinates) {
        double minLatitude = Double.parseDouble(ServiceArea.MIN_LATITUDE);
        double maxLatitude = Double.parseDouble(ServiceArea.MAX_LATITUDE);
        double minLongitude = Double.parseDouble(ServiceArea.MIN_LONGITUDE);
        double maxLongitude = Double.parseDouble(ServiceArea.MAX_LONGITUDE);
        if (coordinates.latitude() < minLatitude || coordinates.latitude() > maxLatitude
                || coordinates.longitude() < minLongitude || coordinates.longitude() > maxLongitude) {
            throw invalid("The Google Maps location must be within the Cairo and Giza service area.");
        }
    }

    private String normalizedUrl(Coordinates coordinates) {
        return String.format(
                Locale.ROOT,
                "https://www.google.com/maps/search/?api=1&query=%s%%2C%s",
                coordinates.latitude(),
                coordinates.longitude());
    }

    private InvalidGoogleMapsLocationException invalid(String message) {
        return new InvalidGoogleMapsLocationException(message);
    }

    private record Coordinates(double latitude, double longitude) {
    }
}
