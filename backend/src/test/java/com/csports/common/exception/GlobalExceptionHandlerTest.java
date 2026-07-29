package com.csports.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.csports.sport.exception.SportNotFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");

    @Test
    void shouldReturnConsistentNotFoundResponse() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Missing entity"),
                request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("RESOURCE_NOT_FOUND", response.getBody().code());
        assertEquals("Missing entity", response.getBody().message());
        assertEquals("/api/v1/test", response.getBody().path());
        assertTrue(response.getBody().fieldErrors().isEmpty());
    }

    @Test
    void shouldHandleSportNotFoundUsingTheSameEnvelope() {
        ResponseEntity<ErrorResponse> response =
                handler.handleNotFound(new SportNotFoundException(), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("RESOURCE_NOT_FOUND", response.getBody().code());
        assertEquals("Sport not found.", response.getBody().message());
    }
}
