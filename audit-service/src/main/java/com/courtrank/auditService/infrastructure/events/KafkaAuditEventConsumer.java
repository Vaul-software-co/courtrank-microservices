package com.courtrank.auditService.infrastructure.events;

import com.courtrank.auditService.application.dto.IngestAuditEventRequest;
import com.courtrank.auditService.application.useCases.IngestAuditEventUseCase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class KafkaAuditEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(KafkaAuditEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final IngestAuditEventUseCase ingestAuditEventUseCase;

    public KafkaAuditEventConsumer(
            ObjectMapper objectMapper,
            IngestAuditEventUseCase ingestAuditEventUseCase
    ) {
        this.objectMapper = objectMapper;
        this.ingestAuditEventUseCase = ingestAuditEventUseCase;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.audit-events}",
            groupId = "${app.kafka.consumer.group-id}"
    )
    public void consume(String message) {
        try {
            this.ingestAuditEventUseCase.execute(this.toRequest(message));
        } catch (RuntimeException exception) {
            log.warn("Ignoring invalid audit event message: {}", exception.getMessage());
        }
    }

    private IngestAuditEventRequest toRequest(String message) {
        try {
            JsonNode root = this.objectMapper.readTree(message);
            JsonNode payload = required(root, "payload");

            return new IngestAuditEventRequest(
                    UUID.fromString(requiredText(root, "eventId")),
                    requiredText(root, "source"),
                    requiredText(payload, "type"),
                    optionalUuid(payload, "actorId"),
                    optionalUuid(payload, "targetId"),
                    optionalText(payload, "traceId"),
                    metadata(payload.get("metadata")),
                    Instant.parse(requiredText(payload, "occurredAt")),
                    Instant.parse(requiredText(root, "publishedAt"))
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid audit event message", exception);
        }
    }

    private JsonNode required(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("Missing field " + field);
        }
        return value;
    }

    private String requiredText(JsonNode node, String field) {
        String value = optionalText(node, field);
        if (value == null) {
            throw new IllegalArgumentException("Missing field " + field);
        }
        return value;
    }

    private String optionalText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private UUID optionalUuid(JsonNode node, String field) {
        String value = optionalText(node, field);
        return value == null ? null : UUID.fromString(value);
    }

    private Map<String, Object> metadata(JsonNode metadata) {
        if (metadata == null || metadata.isNull()) {
            return Map.of();
        }
        return this.objectMapper.convertValue(metadata, new TypeReference<>() {});
    }
}
