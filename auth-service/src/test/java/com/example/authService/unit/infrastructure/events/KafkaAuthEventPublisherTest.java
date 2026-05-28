package com.example.authService.unit.infrastructure.events;

import com.example.authService.application.events.UserDeletedEvent;
import com.example.authService.application.events.UserRegisteredEvent;
import com.example.authService.application.events.UserRestoredEvent;
import com.example.authService.domain.enums.UserRole;
import com.example.authService.infrastructure.events.KafkaAuthEventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class KafkaAuthEventPublisherTest {
    private static final String TOPIC = "auth.events";

    @Test
    void publishUserRegistered_shouldSendUserRegisteredEventToKafka() throws Exception {
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        KafkaAuthEventPublisher publisher = new KafkaAuthEventPublisher(kafkaTemplate, objectMapper, TOPIC);
        UUID userId = UUID.randomUUID();

        publisher.publishUserRegistered(new UserRegisteredEvent(
                userId,
                "test@test.com",
                "Test User",
                "testuser",
                UserRole.MEMBER,
                "v1",
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        ));

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq(userId.toString()), payload.capture());

        JsonNode json = objectMapper.readTree(payload.getValue());
        assertEquals("USER_REGISTERED", json.get("eventType").asText());
        assertEquals(userId.toString(), json.get("aggregateId").asText());
        assertEquals("auth-service", json.get("source").asText());
        assertEquals("test@test.com", json.get("payload").get("email").asText());
    }

    @Test
    void publishUserRestored_shouldSendUserRestoredEventToKafka() throws Exception {
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        KafkaAuthEventPublisher publisher = new KafkaAuthEventPublisher(kafkaTemplate, objectMapper, TOPIC);
        UUID userId = UUID.randomUUID();

        publisher.publishUserRestored(new UserRestoredEvent(
                userId,
                "test@test.com",
                "Test User",
                "testuser",
                UserRole.MEMBER,
                "v1",
                false,
                Instant.parse("2026-01-01T00:00:00Z")
        ));

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq(userId.toString()), payload.capture());

        JsonNode json = objectMapper.readTree(payload.getValue());
        assertEquals("USER_RESTORED", json.get("eventType").asText());
        assertEquals(userId.toString(), json.get("aggregateId").asText());
        assertEquals("test@test.com", json.get("payload").get("email").asText());
    }

    @Test
    void publishUserDeleted_shouldSendUserDeletedEventToKafka() throws Exception {
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        KafkaAuthEventPublisher publisher = new KafkaAuthEventPublisher(kafkaTemplate, objectMapper, TOPIC);
        UUID userId = UUID.randomUUID();

        publisher.publishUserDeleted(new UserDeletedEvent(
                userId,
                "test@test.com",
                Instant.parse("2026-01-01T00:00:00Z")
        ));

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq(userId.toString()), payload.capture());

        JsonNode json = objectMapper.readTree(payload.getValue());
        assertEquals("USER_DELETED", json.get("eventType").asText());
        assertEquals(userId.toString(), json.get("aggregateId").asText());
        assertEquals("test@test.com", json.get("payload").get("email").asText());
    }
}
