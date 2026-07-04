package com.Csports.Csports.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.Csports.Csports.DTO.ErrorResponse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleResourceNotFoundExceptionWithNotFoundStatus() {
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(new ResourceNotFoundException("Missing entity"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Missing entity", response.getBody());
    }

    @Test
    void shouldHandleSportNotFoundExceptionWithNotFoundStatus() {
        ResponseEntity<ErrorResponse> response = handler.handleSportNotFound(new SportNotFoundException());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Sport not found.", response.getBody());
    }
}
