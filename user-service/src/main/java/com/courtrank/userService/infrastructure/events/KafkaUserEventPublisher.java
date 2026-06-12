package com.courtrank.userService.infrastructure.events;

import com.courtrank.userService.application.events.UserProfileChangedEvent;
import com.courtrank.userService.application.events.UserProfileCreatedEvent;
import com.courtrank.userService.application.events.UserProfileDeletedEvent;
import com.courtrank.userService.application.ports.UserEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

public class KafkaUserEventPublisher implements UserEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaUserEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publishUserProfileCreated(UserProfileCreatedEvent event) {
        this.publish("USER_PROFILE_CREATED", event.id(), event);
    }

    @Override
    public void publishUserProfileUpdated(UserProfileChangedEvent event) {
        this.publish("USER_PROFILE_UPDATED", event.id(), event);
    }

    @Override
    public void publishUserProfileDeleted(UserProfileDeletedEvent event) {
        this.publish("USER_PROFILE_DELETED", event.id(), event);
    }

    @Override
    public void publishUserProfileRestored(UserProfileChangedEvent event) {
        this.publish("USER_PROFILE_RESTORED", event.id(), event);
    }

    @Override
    public void publishUserProfileBecamePublic(UserProfileChangedEvent event) {
        this.publish("USER_PROFILE_BECAME_PUBLIC", event.id(), event);
    }

    private void publish(String eventType, UUID aggregateId, Object payload) {
        UserEventMessage message = new UserEventMessage(
                UUID.randomUUID(),
                eventType,
                aggregateId,
                "user-service",
                payload,
                Instant.now()
        );

        try {
            this.kafkaTemplate.send(this.topic, aggregateId.toString(), this.objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize user event " + eventType, exception);
        }
    }

    private record UserEventMessage(
            UUID eventId,
            String eventType,
            UUID aggregateId,
            String source,
            Object payload,
            Instant publishedAt
    ) {
    }
}
