package com.courtrank.authService.infrastructure.audit;

import com.courtrank.authService.application.ports.audit.AuditEvent;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

public class KafkaAuditLogger implements AuditLogger {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaAuditLogger(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void log(AuditEvent event) {
        AuditEventMessage message = new AuditEventMessage(
                UUID.randomUUID(),
                "auth-service",
                event,
                Instant.now()
        );

        try {
            String key = event.targetId() != null ? event.targetId().toString() : event.type().name();
            this.kafkaTemplate.send(this.topic, key, this.objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize audit event " + event.type(), exception);
        }
    }

    private record AuditEventMessage(
            UUID eventId,
            String source,
            AuditEvent payload,
            Instant publishedAt
    ) {
    }
}
