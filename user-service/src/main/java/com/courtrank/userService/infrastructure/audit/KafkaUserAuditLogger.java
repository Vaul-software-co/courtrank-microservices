package com.courtrank.userService.infrastructure.audit;

import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

public class KafkaUserAuditLogger implements UserAuditLogger {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaUserAuditLogger(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void log(UserAuditEvent event) {
        AuditEventMessage message = new AuditEventMessage(
                UUID.randomUUID(),
                "user-service",
                event,
                Instant.now()
        );

        try {
            String key = event.targetId() != null ? event.targetId().toString() : event.type().name();
            this.kafkaTemplate.send(this.topic, key, this.objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize user audit event " + event.type(), exception);
        }
    }

    private record AuditEventMessage(
            UUID eventId,
            String source,
            UserAuditEvent payload,
            Instant publishedAt
    ) {
    }
}
