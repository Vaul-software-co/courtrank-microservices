package com.courtrank.authService.infrastructure.events;

import com.courtrank.authService.application.events.UserDeletedEvent;
import com.courtrank.authService.application.events.UserEmailVerifiedEvent;
import com.courtrank.authService.application.events.UserRegisteredEvent;
import com.courtrank.authService.application.events.UserRestoredEvent;
import com.courtrank.authService.application.ports.AuthEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class KafkaAuthEventPublisher implements AuthEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaAuthEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publishUserRegistered(UserRegisteredEvent event) {
        this.publish("USER_REGISTERED", event.id(), event);
    }

    @Override
    public void publishUserRestored(UserRestoredEvent event) {
        this.publish("USER_RESTORED", event.id(), event);
    }

    @Override
    public void publishUserDeleted(UserDeletedEvent event) {
        this.publish("USER_DELETED", event.id(), event);
    }

    @Override
    public void publishUserEmailVerified(UserEmailVerifiedEvent event) {
        this.publish("USER_EMAIL_VERIFIED", event.id(), event);
    }

    private void publish(String eventType, UUID aggregateId, Object payload) {
        AuthEventMessage message = new AuthEventMessage(
                UUID.randomUUID(),
                eventType,
                aggregateId,
                "auth-service",
                payload,
                Instant.now()
        );

        try {
            this.kafkaTemplate.send(this.topic, aggregateId.toString(), this.objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize auth event " + eventType, exception);
        }
    }

    private record AuthEventMessage(
            UUID eventId,
            String eventType,
            UUID aggregateId,
            String source,
            Object payload,
            Instant publishedAt
    ) {
    }
}
