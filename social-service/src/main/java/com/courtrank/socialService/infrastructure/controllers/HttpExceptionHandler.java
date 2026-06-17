package com.courtrank.socialService.infrastructure.controllers;

import com.courtrank.socialService.domain.exceptions.DomainValidationException;
import com.courtrank.socialService.domain.exceptions.FollowNotFoundException;
import com.courtrank.socialService.domain.exceptions.SocialInteractionBlockedException;
import com.courtrank.socialService.domain.exceptions.SocialUserNotFoundException;
import jakarta.validation.ConstraintViolationException;
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

    @ExceptionHandler({SocialUserNotFoundException.class, FollowNotFoundException.class})
    public ResponseEntity<Map<String, String>> notFound(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(SocialInteractionBlockedException.class)
    public ResponseEntity<Map<String, String>> forbidden(SocialInteractionBlockedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<Map<String, String>> badRequest(DomainValidationException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("error", "Validation failed", "fields", fields));
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
