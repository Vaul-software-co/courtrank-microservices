package com.example.userService.infrastructure.events;

import com.example.userService.application.dto.CreateUserRequest;
import com.example.userService.application.useCases.CreateUserFromAuthEventUseCase;
import com.example.userService.application.useCases.DeleteUserFromAuthEventUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile({"kafka", "production"})
public class AuthEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(AuthEventConsumer.class);
    private static final String USER_REGISTERED = "USER_REGISTERED";
    private static final String USER_RESTORED = "USER_RESTORED";
    private static final String USER_DELETED = "USER_DELETED";

    private final ObjectMapper objectMapper;
    private final CreateUserFromAuthEventUseCase createUserFromAuthEventUseCase;
    private final DeleteUserFromAuthEventUseCase deleteUserFromAuthEventUseCase;

    public AuthEventConsumer(
            ObjectMapper objectMapper,
            CreateUserFromAuthEventUseCase createUserFromAuthEventUseCase, DeleteUserFromAuthEventUseCase deleteUserFromAuthEventUseCase
    ) {
        this.objectMapper = objectMapper;
        this.createUserFromAuthEventUseCase = createUserFromAuthEventUseCase;
        this.deleteUserFromAuthEventUseCase = deleteUserFromAuthEventUseCase;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.auth-events}",
            groupId = "${app.kafka.consumer.group-id}"
    )
    public void consume(String message) {
        AuthEventMessage event = this.parse(message);

        if (USER_REGISTERED.equals(event.eventType()) || USER_RESTORED.equals(event.eventType())) {
            logger.info(
                    "Processing auth event type={} eventId={} aggregateId={}",
                    event.eventType(),
                    event.eventId(),
                    event.aggregateId()
            );
            this.createUserFromAuthEventUseCase.execute(this.toCreateUserRequest(event.payload()));
            logger.info(
                    "Processed auth event type={} eventId={} aggregateId={}",
                    event.eventType(),
                    event.eventId(),
                    event.aggregateId()
            );
            return;
        }

        if (USER_DELETED.equals(event.eventType())) {
            logger.info(
                    "Processing auth event type={} eventId={} aggregateId={}",
                    event.eventType(),
                    event.eventId(),
                    event.aggregateId()
            );
            this.deleteUserFromAuthEventUseCase.execute(event.aggregateId());
            logger.info(
                    "Processed auth event type={} eventId={} aggregateId={}",
                    event.eventType(),
                    event.eventId(),
                    event.aggregateId()
            );
            return;
        }

        logger.debug("Ignoring auth event type={}", event.eventType());
    }

    private AuthEventMessage parse(String message) {
        try {
            return this.objectMapper.readValue(message, AuthEventMessage.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid auth event payload", exception);
        }
    }

    private CreateUserRequest toCreateUserRequest(JsonNode payload) {
        return new CreateUserRequest(
                UUID.fromString(payload.required("id").asText()),
                payload.required("name").asText(),
                this.nullableText(payload.get("username")),
                payload.required("email").asText()
        );
    }

    private String nullableText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        String value = node.asText();
        return value.isBlank() ? null : value;
    }

    private record AuthEventMessage(
            UUID eventId,
            String eventType,
            UUID aggregateId,
            String source,
            JsonNode payload,
            String publishedAt
    ) {
    }
}
