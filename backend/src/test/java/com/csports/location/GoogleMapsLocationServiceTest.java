package com.csports.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.csports.location.dto.ResolvedGoogleMapsLocationResponse;
import com.csports.location.exception.InvalidGoogleMapsLocationException;

class GoogleMapsLocationServiceTest {

    private final GoogleMapsLocationService service = new GoogleMapsLocationService();

    @Test
    void resolvesCommonGoogleMapsCoordinateFormats() {
        ResolvedGoogleMapsLocationResponse atLocation = service.resolve(
                "https://www.google.com/maps/place/Cairo/@30.0444,31.2357,15z");
        ResolvedGoogleMapsLocationResponse queryLocation = service.resolve(
                "https://www.google.com/maps?q=30.069193,31.312347");
        ResolvedGoogleMapsLocationResponse dataLocation = service.resolve(
                "https://www.google.com/maps/place/Test/data=!3d30.0529854!4d31.2232786");

        assertThat(atLocation.latitude()).isEqualTo(30.0444);
        assertThat(queryLocation.longitude()).isEqualTo(31.312347);
        assertThat(dataLocation.normalizedGoogleMapsUrl())
                .isEqualTo("https://www.google.com/maps/search/?api=1&query=30.0529854%2C31.2232786");
    }

    @Test
    void rejectsNonGoogleAndOutOfAreaLocations() {
        assertThatThrownBy(() -> service.resolve("https://example.com/maps?q=30.0444,31.2357"))
                .isInstanceOf(InvalidGoogleMapsLocationException.class)
                .hasMessage("Enter a valid HTTPS Google Maps link.");
        assertThatThrownBy(() -> service.resolve("https://www.google.com/maps?q=40.7128,-74.0060"))
                .isInstanceOf(InvalidGoogleMapsLocationException.class)
                .hasMessageContaining("Cairo and Giza");
    }
}
