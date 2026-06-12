package com.courtrank.socialService.infrastructure.events;

import com.courtrank.socialService.application.dto.DeleteSocialUserRequest;
import com.courtrank.socialService.application.dto.SocialUserSnapshot;
import com.courtrank.socialService.application.dto.SyncSocialUserRequest;
import com.courtrank.socialService.application.useCases.CreateSocialUserFromUserEventUseCase;
import com.courtrank.socialService.application.useCases.DeleteSocialUserFromUserEventUseCase;
import com.courtrank.socialService.application.useCases.HandleUserBecamePublicUseCase;
import com.courtrank.socialService.application.useCases.RestoreSocialUserFromUserEventUseCase;
import com.courtrank.socialService.application.useCases.UpdateSocialUserFromUserEventUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Profile({"kafka", "production"})
public class UserProfileEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(UserProfileEventConsumer.class);
    private static final String USER_PROFILE_CREATED = "USER_PROFILE_CREATED";
    private static final String USER_PROFILE_UPDATED = "USER_PROFILE_UPDATED";
    private static final String USER_PROFILE_DELETED = "USER_PROFILE_DELETED";
    private static final String USER_PROFILE_RESTORED = "USER_PROFILE_RESTORED";
    private static final String USER_PROFILE_BECAME_PUBLIC = "USER_PROFILE_BECAME_PUBLIC";

    private final ObjectMapper objectMapper;
    private final CreateSocialUserFromUserEventUseCase createUseCase;
    private final UpdateSocialUserFromUserEventUseCase updateUseCase;
    private final DeleteSocialUserFromUserEventUseCase deleteUseCase;
    private final RestoreSocialUserFromUserEventUseCase restoreUseCase;
    private final HandleUserBecamePublicUseCase handleUserBecamePublicUseCase;

    public UserProfileEventConsumer(
            ObjectMapper objectMapper,
            CreateSocialUserFromUserEventUseCase createUseCase,
            UpdateSocialUserFromUserEventUseCase updateUseCase,
            DeleteSocialUserFromUserEventUseCase deleteUseCase,
            RestoreSocialUserFromUserEventUseCase restoreUseCase,
            HandleUserBecamePublicUseCase handleUserBecamePublicUseCase
    ) {
        this.objectMapper = objectMapper;
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.restoreUseCase = restoreUseCase;
        this.handleUserBecamePublicUseCase = handleUserBecamePublicUseCase;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.user-events}",
            groupId = "${app.kafka.consumer.group-id}"
    )
    public void consume(String message) {
        UserEventMessage event = this.parse(message);
        logger.info("Processing user event type={} eventId={} aggregateId={}", event.eventType(), event.eventId(), event.aggregateId());

        if (USER_PROFILE_CREATED.equals(event.eventType())) {
            this.createUseCase.execute(new SyncSocialUserRequest(this.toSnapshot(event.payload(), event.publishedAt())));
        } else if (USER_PROFILE_UPDATED.equals(event.eventType())) {
            this.updateUseCase.execute(new SyncSocialUserRequest(this.toSnapshot(event.payload(), event.publishedAt())));
        } else if (USER_PROFILE_DELETED.equals(event.eventType())) {
            Instant occurredAt = this.instant(event.payload().get("occurredAt"), event.publishedAt());
            this.deleteUseCase.execute(new DeleteSocialUserRequest(event.aggregateId(), occurredAt, occurredAt));
        } else if (USER_PROFILE_RESTORED.equals(event.eventType())) {
            this.restoreUseCase.execute(new SyncSocialUserRequest(this.toSnapshot(event.payload(), event.publishedAt())));
        } else if (USER_PROFILE_BECAME_PUBLIC.equals(event.eventType())) {
            this.handleUserBecamePublicUseCase.execute(event.aggregateId());
        } else {
            logger.debug("Ignoring user event type={}", event.eventType());
            return;
        }

        logger.info("Processed user event type={} eventId={} aggregateId={}", event.eventType(), event.eventId(), event.aggregateId());
    }

    private UserEventMessage parse(String message) {
        try {
            return this.objectMapper.readValue(message, UserEventMessage.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid user event payload", exception);
        }
    }

    private SocialUserSnapshot toSnapshot(JsonNode payload, Instant fallbackSourceUpdatedAt) {
        UUID userId = UUID.fromString(this.firstText(payload, "id", "userId"));
        String status = this.text(payload.get("status"));
        boolean active = status == null || "VISIBLE".equals(status) || "ACTIVE".equals(status);
        Instant occurredAt = this.instant(payload.get("occurredAt"), fallbackSourceUpdatedAt);

        return new SocialUserSnapshot(
                userId,
                payload.required("name").asText(),
                this.text(payload.get("username")),
                this.text(payload.get("avatarUrl")),
                payload.path("privateProfile").asBoolean(false),
                active,
                active ? null : occurredAt,
                occurredAt
        );
    }

    private String firstText(JsonNode payload, String first, String second) {
        JsonNode firstNode = payload.get(first);
        if (firstNode != null && !firstNode.isNull()) {
            return firstNode.asText();
        }

        return payload.required(second).asText();
    }

    private String text(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        String value = node.asText();
        return value.isBlank() ? null : value;
    }

    private Instant instant(JsonNode node, Instant fallback) {
        String value = this.text(node);
        return value == null ? fallback : Instant.parse(value);
    }

    private record UserEventMessage(
            UUID eventId,
            String eventType,
            UUID aggregateId,
            String source,
            JsonNode payload,
            Instant publishedAt
    ) {
    }
}
