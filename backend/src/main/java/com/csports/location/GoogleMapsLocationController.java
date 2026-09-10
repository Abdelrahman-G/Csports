package com.csports.location;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.csports.common.web.ApiPaths;
import com.csports.location.dto.ResolveGoogleMapsLocationRequest;
import com.csports.location.dto.ResolvedGoogleMapsLocationResponse;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping(ApiPaths.LOCATIONS)
@PreAuthorize("hasRole('TRAINER')")
public class GoogleMapsLocationController {

    private final GoogleMapsLocationService googleMapsLocationService;

    public GoogleMapsLocationController(GoogleMapsLocationService googleMapsLocationService) {
        this.googleMapsLocationService = googleMapsLocationService;
    }

    @PostMapping("/google-maps/resolve")
    public ResolvedGoogleMapsLocationResponse resolveGoogleMapsLocation(
            @Valid @RequestBody ResolveGoogleMapsLocationRequest request) {
        return googleMapsLocationService.resolve(request.url());
    }
}
