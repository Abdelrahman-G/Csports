package com.csports.common.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.csports.auth.exception.EmailAlreadyExistsException;
import com.csports.auth.exception.InvalidCredentialsException;
import com.csports.auth.exception.PhoneNumberAlreadyExistsException;
import com.csports.booking.exception.AlreadyBookedException;
import com.csports.booking.exception.CannotBookOwnSessionException;
import com.csports.booking.exception.SessionFullException;
import com.csports.session.exception.InvalidSessionScheduleException;
import com.csports.session.exception.SessionEndedException;
import com.csports.session.exception.SessionStateConflictException;
import com.csports.sport.exception.SportNotFoundException;
import com.csports.trainer.exception.TrainerProfileAlreadyExistsException;
import com.csports.trainer.exception.TrainerProfileNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request) {
        return buildResponse(status, code, message, request, Map.of());
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                status.value(),
                code,
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                Instant.now(),
                fieldErrors));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "The request contains invalid values.",
                request,
                errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(
            HandlerMethodValidationException ex,
            HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> {
            String parameterName = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(error ->
                    errors.putIfAbsent(
                            parameterName == null ? "parameter" : parameterName,
                            error.getDefaultMessage()));
        });
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "The request contains invalid values.",
                request,
                errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String path = violation.getPropertyPath().toString();
            String field = path.substring(path.lastIndexOf('.') + 1);
            errors.putIfAbsent(field, violation.getMessage());
        }
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "The request contains invalid values.",
                request,
                errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                "The request body is missing or contains invalid JSON values.",
                request);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidParameter(
            Exception ex,
            HttpServletRequest request) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                "A request parameter has a missing or invalid value.",
                request);
    }

    @ExceptionHandler({
            EmailAlreadyExistsException.class,
            PhoneNumberAlreadyExistsException.class,
            AlreadyBookedException.class,
            SessionFullException.class,
            SessionStateConflictException.class,
            ObjectOptimisticLockingFailureException.class,
            DataIntegrityViolationException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(
            RuntimeException ex,
            HttpServletRequest request) {
        String message = ex instanceof ObjectOptimisticLockingFailureException
                ? "The resource was changed by another request. Please retry."
                : ex instanceof DataIntegrityViolationException
                        ? "The request conflicts with existing data."
                        : ex.getMessage();
        return buildResponse(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", message, request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage(), request);
    }

    @ExceptionHandler({
            ResourceNotFoundException.class,
            SportNotFoundException.class,
            TrainerProfileNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(
            RuntimeException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler({
            InvalidSessionScheduleException.class,
            SessionEndedException.class,
            CannotBookOwnSessionException.class
    })
    public ResponseEntity<ErrorResponse> handleBusinessValidation(
            RuntimeException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION", ex.getMessage(), request);
    }

    @ExceptionHandler({
            ForbiddenOperationException.class,
            AccessDeniedException.class
    })
    public ResponseEntity<ErrorResponse> handleForbidden(
            RuntimeException ex,
            HttpServletRequest request) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "You do not have permission to perform this action.",
                request);
    }

    @ExceptionHandler(TrainerProfileAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleTrainerProfileAlreadyExists(
            TrainerProfileAlreadyExistsException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(
            NoResourceFoundException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "ENDPOINT_NOT_FOUND", "The requested endpoint does not exist.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                "This HTTP method is not supported for the requested endpoint.",
                request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        return buildResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE",
                "The request Content-Type is not supported.",
                request);
    }

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            GeneralException ex,
            HttpServletRequest request) {
        log.error("Unexpected general application error", ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred.",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request) {
        log.error("Unhandled exception while processing {}", request.getRequestURI(), ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred.",
                request);
    }
}
