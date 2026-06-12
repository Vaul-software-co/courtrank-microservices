package com.courtrank.socialService.unit.infrastructure.events;

import com.courtrank.socialService.application.events.FollowAcceptedEvent;
import com.courtrank.socialService.application.events.FollowRejectedEvent;
import com.courtrank.socialService.application.events.FollowRemovedEvent;
import com.courtrank.socialService.application.events.FollowRequestedEvent;
import com.courtrank.socialService.application.events.FollowerRemovedEvent;
import com.courtrank.socialService.application.events.UserBlockedEvent;
import com.courtrank.socialService.application.events.UserUnblockedEvent;
import com.courtrank.socialService.infrastructure.events.KafkaSocialEventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KafkaSocialEventPublisherTest {
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private KafkaTemplate<String, String> kafkaTemplate;
    private KafkaSocialEventPublisher publisher;

    @BeforeEach
    void setUp() {
        this.kafkaTemplate = mock();
        this.publisher = new KafkaSocialEventPublisher(this.kafkaTemplate, this.objectMapper, "social.events");
    }

    @Test
    void publishFollowRequested_shouldPublishMessageWithAggregateKey() throws Exception {
        UUID followId = UUID.randomUUID();
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();

        this.publisher.publishFollowRequested(new FollowRequestedEvent(
                followId,
                followerId,
                followingId,
                Instant.parse("2026-06-02T10:15:30Z")
        ));

        JsonNode message = captureMessage();
        assertThat(message.get("eventType").asText()).isEqualTo("FOLLOW_REQUESTED");
        assertThat(message.get("aggregateId").asText()).isEqualTo(followId.toString());
        assertThat(message.get("source").asText()).isEqualTo("social-service");
        assertThat(message.get("payload").get("followerId").asText()).isEqualTo(followerId.toString());
        assertThat(message.get("payload").get("followingId").asText()).isEqualTo(followingId.toString());
    }

    @Test
    void publishFollowAccepted_shouldPublishMessageWithAggregateKey() throws Exception {
        UUID followId = UUID.randomUUID();

        this.publisher.publishFollowAccepted(new FollowAcceptedEvent(
                followId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-06-02T10:15:30Z")
        ));

        JsonNode message = captureMessage();
        assertThat(message.get("eventType").asText()).isEqualTo("FOLLOW_ACCEPTED");
        assertThat(message.get("aggregateId").asText()).isEqualTo(followId.toString());
    }

    @Test
    void publishFollowRejected_shouldPublishMessageWithAggregateKey() throws Exception {
        UUID followId = UUID.randomUUID();

        this.publisher.publishFollowRejected(new FollowRejectedEvent(
                followId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-06-02T10:15:30Z")
        ));

        JsonNode message = captureMessage();
        assertThat(message.get("eventType").asText()).isEqualTo("FOLLOW_REJECTED");
        assertThat(message.get("aggregateId").asText()).isEqualTo(followId.toString());
    }

    @Test
    void publishFollowRemoved_shouldPublishMessageWithAggregateKey() throws Exception {
        UUID followId = UUID.randomUUID();
        UUID removedByUserId = UUID.randomUUID();

        this.publisher.publishFollowRemoved(new FollowRemovedEvent(
                followId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                removedByUserId,
                Instant.parse("2026-06-02T10:15:30Z")
        ));

        JsonNode message = captureMessage();
        assertThat(message.get("eventType").asText()).isEqualTo("FOLLOW_REMOVED");
        assertThat(message.get("payload").get("removedByUserId").asText()).isEqualTo(removedByUserId.toString());
    }

    @Test
    void publishFollowerRemoved_shouldPublishMessageWithAggregateKey() throws Exception {
        UUID followId = UUID.randomUUID();

        this.publisher.publishFollowerRemoved(new FollowerRemovedEvent(
                followId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-06-02T10:15:30Z")
        ));

        JsonNode message = captureMessage();
        assertThat(message.get("eventType").asText()).isEqualTo("FOLLOWER_REMOVED");
        assertThat(message.get("aggregateId").asText()).isEqualTo(followId.toString());
    }

    @Test
    void publishUserBlocked_shouldPublishMessageWithAggregateKey() throws Exception {
        UUID blockId = UUID.randomUUID();
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();

        this.publisher.publishUserBlocked(new UserBlockedEvent(
                blockId,
                blockerId,
                blockedId,
                Instant.parse("2026-06-02T10:15:30Z")
        ));

        JsonNode message = captureMessage();
        assertThat(message.get("eventType").asText()).isEqualTo("USER_BLOCKED");
        assertThat(message.get("aggregateId").asText()).isEqualTo(blockId.toString());
        assertThat(message.get("payload").get("blockerId").asText()).isEqualTo(blockerId.toString());
        assertThat(message.get("payload").get("blockedId").asText()).isEqualTo(blockedId.toString());
    }

    @Test
    void publishUserUnblocked_shouldPublishMessageWithAggregateKey() throws Exception {
        UUID blockId = UUID.randomUUID();

        this.publisher.publishUserUnblocked(new UserUnblockedEvent(
                blockId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-06-02T10:15:30Z")
        ));

        JsonNode message = captureMessage();
        assertThat(message.get("eventType").asText()).isEqualTo("USER_UNBLOCKED");
        assertThat(message.get("aggregateId").asText()).isEqualTo(blockId.toString());
    }

    private JsonNode captureMessage() throws Exception {
        ArgumentCaptor<String> topic = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(this.kafkaTemplate).send(topic.capture(), key.capture(), payload.capture());

        JsonNode message = this.objectMapper.readTree(payload.getValue());
        assertThat(topic.getValue()).isEqualTo("social.events");
        assertThat(key.getValue()).isEqualTo(message.get("aggregateId").asText());
        assertThat(message.get("eventId").asText()).isNotBlank();
        assertThat(message.get("publishedAt").asText()).isNotBlank();
        return message;
    }
}
