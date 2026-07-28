package com.csports.common.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.csports.auth.exception.EmailAlreadyExistsException;
import com.csports.auth.exception.InvalidCredentialsException;
import com.csports.auth.exception.PhoneNumberAlreadyExistsException;
import com.csports.booking.exception.AlreadyBookedException;
import com.csports.booking.exception.CannotBookOwnSessionException;
import com.csports.booking.exception.SessionFullException;
import com.csports.sport.exception.SportNotFoundException;
import com.csports.trainer.exception.TrainerProfileAlreadyExistsException;
import com.csports.trainer.exception.TrainerProfileNotFoundException;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {

        return ResponseEntity.status(status).body(new ErrorResponse(
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PhoneNumberAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handlePhoneNumberAlreadyExists(PhoneNumberAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(TrainerProfileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTrainerProfileNotFound(TrainerProfileNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AlreadyBookedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyBooked(AlreadyBookedException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(SessionFullException.class)
    public ResponseEntity<ErrorResponse> handleSessionFull(SessionFullException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CannotBookOwnSessionException.class)
    public ResponseEntity<ErrorResponse> handleOwnSession(CannotBookOwnSessionException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SportNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSportNotFound(SportNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TrainerProfileAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleTrainerProfileAlreadyExists(TrainerProfileAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(GeneralException ex) {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage());
    }
}
