package com.courtrank.socialService.infrastructure.audit;

import com.courtrank.socialService.application.ports.audit.SocialAuditEvent;
import com.courtrank.socialService.application.ports.audit.SocialAuditLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

public class KafkaSocialAuditLogger implements SocialAuditLogger {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaSocialAuditLogger(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void log(SocialAuditEvent event) {
        AuditEventMessage message = new AuditEventMessage(
                UUID.randomUUID(),
                "social-service",
                event,
                Instant.now()
        );

        try {
            String key = event.targetId() != null ? event.targetId().toString() : event.type().name();
            this.kafkaTemplate.send(this.topic, key, this.objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize social audit event " + event.type(), exception);
        }
    }

    private record AuditEventMessage(
            UUID eventId,
            String source,
            SocialAuditEvent payload,
            Instant publishedAt
    ) {
    }
}
