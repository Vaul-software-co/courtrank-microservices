package com.courtrank.userService.unit.infrastructure.audit;

import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditEventType;
import com.courtrank.userService.infrastructure.audit.KafkaUserAuditLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KafkaUserAuditLoggerTest {
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private KafkaTemplate<String, String> kafkaTemplate;
    private KafkaUserAuditLogger auditLogger;

    @BeforeEach
    void setUp() {
        this.kafkaTemplate = mock();
        this.auditLogger = new KafkaUserAuditLogger(this.kafkaTemplate, this.objectMapper, "audit.events");
    }

    @Test
    void log_shouldPublishAuditMessageWithTargetIdAsKey() throws Exception {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UserAuditEvent event = new UserAuditEvent(
                UserAuditEventType.USER_PROFILE_UPDATED,
                actorId,
                targetId,
                "trace-123",
                Map.of("field", "name"),
                Instant.parse("2026-06-02T10:15:30Z")
        );

        this.auditLogger.log(event);

        ArgumentCaptor<String> topic = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(this.kafkaTemplate).send(topic.capture(), key.capture(), payload.capture());

        JsonNode message = this.objectMapper.readTree(payload.getValue());
        assertThat(topic.getValue()).isEqualTo("audit.events");
        assertThat(key.getValue()).isEqualTo(targetId.toString());
        assertThat(message.get("source").asText()).isEqualTo("user-service");
        assertThat(message.get("payload").get("type").asText()).isEqualTo("USER_PROFILE_UPDATED");
        assertThat(message.get("payload").get("actorId").asText()).isEqualTo(actorId.toString());
        assertThat(message.get("payload").get("targetId").asText()).isEqualTo(targetId.toString());
        assertThat(message.get("payload").get("traceId").asText()).isEqualTo("trace-123");
        assertThat(message.get("payload").get("metadata").get("field").asText()).isEqualTo("name");
    }

    @Test
    void log_shouldUseEventTypeAsKeyWhenTargetIdIsMissing() throws Exception {
        UserAuditEvent event = new UserAuditEvent(
                UserAuditEventType.USER_PROFILE_CREATION_FAILED_USERNAME_CONFLICT,
                UUID.randomUUID(),
                null,
                "trace-456",
                Map.of(),
                Instant.parse("2026-06-02T10:15:30Z")
        );

        this.auditLogger.log(event);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(this.kafkaTemplate).send(
                org.mockito.ArgumentMatchers.eq("audit.events"),
                key.capture(),
                org.mockito.ArgumentMatchers.anyString()
        );

        assertThat(key.getValue()).isEqualTo("USER_PROFILE_CREATION_FAILED_USERNAME_CONFLICT");
    }
}
