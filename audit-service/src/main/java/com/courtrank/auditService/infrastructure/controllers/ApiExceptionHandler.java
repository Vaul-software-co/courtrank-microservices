package com.courtrank.auditService.infrastructure.controllers;

import com.courtrank.auditService.domain.exceptions.AuditEventNotFoundException;
import com.courtrank.auditService.domain.exceptions.DomainValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AuditEventNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(AuditEventNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler({DomainValidationException.class, MethodArgumentNotValidException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(Exception exception) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", exception.getMessage()));
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
