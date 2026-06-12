package com.courtrank.socialService.infrastructure.events;

import com.courtrank.socialService.application.events.FollowAcceptedEvent;
import com.courtrank.socialService.application.events.FollowRejectedEvent;
import com.courtrank.socialService.application.events.FollowRemovedEvent;
import com.courtrank.socialService.application.events.FollowRequestedEvent;
import com.courtrank.socialService.application.events.FollowerRemovedEvent;
import com.courtrank.socialService.application.events.UserBlockedEvent;
import com.courtrank.socialService.application.events.UserUnblockedEvent;
import com.courtrank.socialService.application.ports.SocialEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

public class KafkaSocialEventPublisher implements SocialEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaSocialEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publishFollowRequested(FollowRequestedEvent event) {
        this.publish("FOLLOW_REQUESTED", event.followId(), event);
    }

    @Override
    public void publishFollowAccepted(FollowAcceptedEvent event) {
        this.publish("FOLLOW_ACCEPTED", event.followId(), event);
    }

    @Override
    public void publishFollowRejected(FollowRejectedEvent event) {
        this.publish("FOLLOW_REJECTED", event.followId(), event);
    }

    @Override
    public void publishFollowRemoved(FollowRemovedEvent event) {
        this.publish("FOLLOW_REMOVED", event.followId(), event);
    }

    @Override
    public void publishFollowerRemoved(FollowerRemovedEvent event) {
        this.publish("FOLLOWER_REMOVED", event.followId(), event);
    }

    @Override
    public void publishUserBlocked(UserBlockedEvent event) {
        this.publish("USER_BLOCKED", event.blockId(), event);
    }

    @Override
    public void publishUserUnblocked(UserUnblockedEvent event) {
        this.publish("USER_UNBLOCKED", event.blockId(), event);
    }

    private void publish(String eventType, UUID aggregateId, Object payload) {
        SocialEventMessage message = new SocialEventMessage(
                UUID.randomUUID(),
                eventType,
                aggregateId,
                "social-service",
                payload,
                Instant.now()
        );

        try {
            this.kafkaTemplate.send(this.topic, aggregateId.toString(), this.objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize social event " + eventType, exception);
        }
    }

    private record SocialEventMessage(
            UUID eventId,
            String eventType,
            UUID aggregateId,
            String source,
            Object payload,
            Instant publishedAt
    ) {
    }
}
