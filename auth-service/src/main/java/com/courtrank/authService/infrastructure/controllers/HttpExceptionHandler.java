package com.courtrank.authService.infrastructure.controllers;

import com.courtrank.authService.domain.exceptions.ConflictException;
import com.courtrank.authService.domain.exceptions.DisabledAccountException;
import com.courtrank.authService.domain.exceptions.EmailNotVerifiedException;
import com.courtrank.authService.domain.exceptions.ForbiddenException;
import com.courtrank.authService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.authService.domain.exceptions.MissedTermsAndConditionsException;
import com.courtrank.authService.domain.exceptions.UserServiceUnavailableException;
import com.courtrank.authService.domain.exceptions.WeakPasswordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class HttpExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(HttpExceptionHandler.class);

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> conflict(ConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler({
            InvalidCredentialsException.class,
            DisabledAccountException.class
    })
    public ResponseEntity<Map<String, String>> unauthorized(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, String>> emailNotVerified(EmailNotVerifiedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "code", "EMAIL_NOT_VERIFIED",
                "error", exception.getMessage()
        ));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> forbidden(ForbiddenException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(UserServiceUnavailableException.class)
    public ResponseEntity<Map<String, String>> serviceUnavailable(UserServiceUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler({
            MissedTermsAndConditionsException.class,
            WeakPasswordException.class
    })
    public ResponseEntity<Map<String, String>> badRequest(RuntimeException exception) {
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> malformedJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid request body"));
    }

    /**
     * Kafka-side failures (broker down, ACL rejection, send timeout) should
     * surface as 503 rather than a generic 500: the caller's request was
     * well-formed; the downstream event bus is degraded.
     *
     * Catches both Spring's KafkaException (wraps producer failures from
     * KafkaTemplate.send().get()) and Apache's KafkaException base class
     * (raw client-side errors). We log the cause so the broker error
     * (TopicAuthorizationException, TimeoutException, …) is visible.
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
