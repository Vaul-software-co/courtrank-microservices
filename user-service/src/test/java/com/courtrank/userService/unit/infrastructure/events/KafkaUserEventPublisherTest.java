package com.courtrank.userService.unit.infrastructure.events;

import com.courtrank.userService.application.events.UserProfileCreatedEvent;
import com.courtrank.userService.domain.enums.UserProfileStatus;
import com.courtrank.userService.infrastructure.events.KafkaUserEventPublisher;
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

class KafkaUserEventPublisherTest {
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private KafkaTemplate<String, String> kafkaTemplate;
    private KafkaUserEventPublisher publisher;

    @BeforeEach
    void setUp() {
        this.kafkaTemplate = mock();
        this.publisher = new KafkaUserEventPublisher(this.kafkaTemplate, this.objectMapper, "user.events");
    }

    @Test
    void publishUserProfileCreated_shouldPublishMessageWithAggregateKey() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileCreatedEvent event = new UserProfileCreatedEvent(
                userId,
                "ana@example.com",
                "Ana Perez",
                "ana",
                false,
                UserProfileStatus.VISIBLE,
                Instant.parse("2026-06-02T10:15:30Z")
        );

        this.publisher.publishUserProfileCreated(event);

        ArgumentCaptor<String> topic = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(this.kafkaTemplate).send(topic.capture(), key.capture(), payload.capture());

        JsonNode message = this.objectMapper.readTree(payload.getValue());
        assertThat(topic.getValue()).isEqualTo("user.events");
        assertThat(key.getValue()).isEqualTo(userId.toString());
        assertThat(message.get("eventType").asText()).isEqualTo("USER_PROFILE_CREATED");
        assertThat(message.get("aggregateId").asText()).isEqualTo(userId.toString());
        assertThat(message.get("source").asText()).isEqualTo("user-service");
        assertThat(message.get("payload").get("email").asText()).isEqualTo("ana@example.com");
        assertThat(message.get("payload").get("username").asText()).isEqualTo("ana");
    }
}
