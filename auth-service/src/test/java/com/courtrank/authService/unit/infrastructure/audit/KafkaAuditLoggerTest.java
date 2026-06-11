package com.courtrank.authService.unit.infrastructure.audit;

import com.courtrank.authService.application.ports.audit.AuditEvent;
import com.courtrank.authService.application.ports.audit.AuditEventType;
import com.courtrank.authService.infrastructure.audit.KafkaAuditLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class KafkaAuditLoggerTest {
    private static final String TOPIC = "audit.events";

    @Test
    void log_shouldSendAuditEventToKafka() throws Exception {
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        KafkaAuditLogger logger = new KafkaAuditLogger(kafkaTemplate, objectMapper, TOPIC);
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        logger.log(new AuditEvent(
                AuditEventType.AUTH_SIGN_IN_SUCCESS,
                actorId,
                targetId,
                "trace-1",
                Map.of("client", "web"),
                Instant.parse("2026-01-01T00:00:00Z")
        ));

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq(targetId.toString()), payload.capture());

        JsonNode json = objectMapper.readTree(payload.getValue());
        assertEquals("auth-service", json.get("source").asText());
        assertEquals("AUTH_SIGN_IN_SUCCESS", json.get("payload").get("type").asText());
        assertEquals(actorId.toString(), json.get("payload").get("actorId").asText());
        assertEquals(targetId.toString(), json.get("payload").get("targetId").asText());
        assertEquals("web", json.get("payload").get("metadata").get("client").asText());
    }

    @Test
    void log_shouldUseEventTypeAsKeyWhenTargetIdIsNull() throws Exception {
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        KafkaAuditLogger logger = new KafkaAuditLogger(kafkaTemplate, objectMapper, TOPIC);

        logger.log(new AuditEvent(
                AuditEventType.AUTH_SIGN_IN_FAILED,
                null,
                null,
                null,
                Map.of("reason", "INVALID_CREDENTIALS"),
                Instant.parse("2026-01-01T00:00:00Z")
        ));

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq(AuditEventType.AUTH_SIGN_IN_FAILED.name()), payload.capture());

        JsonNode json = objectMapper.readTree(payload.getValue());
        assertEquals("AUTH_SIGN_IN_FAILED", json.get("payload").get("type").asText());
    }
}
