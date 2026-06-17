package com.courtrank.userService.infrastructure.controllers;

import com.courtrank.userService.domain.exceptions.DomainValidationException;
import com.courtrank.userService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.userService.domain.exceptions.UserNameAlreadyTakenException;
import com.courtrank.userService.domain.exceptions.UserProfileNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class HttpExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(HttpExceptionHandler.class);

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> unauthorized(InvalidCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(UserProfileNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(UserProfileNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(UserNameAlreadyTakenException.class)
    public ResponseEntity<Map<String, String>> conflict(UserNameAlreadyTakenException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<Map<String, String>> badRequest(DomainValidationException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();

        exception.getBindingResult().getFieldErrors().forEach(error ->
                fields.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation failed",
                "fields", fields
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> constraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", "Validation failed"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> malformedJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid request body"));
    }

    /**
     * Kafka producer/consumer failures should surface as 503 (degraded
     * downstream) rather than a generic 500. Covers both Spring's wrapper
     * and the raw Apache client base class.
     */
    @ExceptionHandler({
            org.springframework.kafka.KafkaException.class,
            org.apache.kafka.common.KafkaException.class
    })
    public ResponseEntity<Map<String, String>> kafkaUnavailable(Exception exception) {
        log.error("Event bus unavailable: {}", exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Event bus unavailable"));
    }
}
