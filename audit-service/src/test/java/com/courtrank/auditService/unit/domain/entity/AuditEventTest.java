package com.courtrank.auditService.unit.domain.entity;

import com.courtrank.auditService.domain.entity.AuditEvent;
import com.courtrank.auditService.domain.enums.AuditEventSource;
import com.courtrank.auditService.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditEventTest {
    @Test
    void ingest_shouldCreateAuditEventWithIngestedAt() {
        UUID eventId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-06-13T10:00:00Z");
        Instant publishedAt = Instant.parse("2026-06-13T10:00:01Z");

        AuditEvent event = AuditEvent.ingest(
                eventId,
                AuditEventSource.AUTH_SERVICE.value(),
                "AUTH_SIGN_IN_SUCCESS",
                actorId,
                targetId,
                "trace-1",
                Map.of("ip", "127.0.0.1"),
                occurredAt,
                publishedAt
        );

        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getSource()).isEqualTo("auth-service");
        assertThat(event.getType()).isEqualTo("AUTH_SIGN_IN_SUCCESS");
        assertThat(event.getActorId()).isEqualTo(actorId);
        assertThat(event.getTargetId()).isEqualTo(targetId);
        assertThat(event.getTraceId()).isEqualTo("trace-1");
        assertThat(event.getMetadata()).containsEntry("ip", "127.0.0.1");
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(event.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(event.getIngestedAt()).isNotNull();
    }

    @Test
    void ingest_shouldDefaultMetadataToEmptyMap() {
        AuditEvent event = AuditEvent.ingest(
                UUID.randomUUID(),
                AuditEventSource.USER_SERVICE.value(),
                "USER_PROFILE_UPDATED",
                null,
                UUID.randomUUID(),
                null,
                null,
                Instant.parse("2026-06-13T10:00:00Z"),
                Instant.parse("2026-06-13T10:00:01Z")
        );

        assertThat(event.getMetadata()).isEmpty();
    }

    @Test
    void restore_shouldRoundTripStoredFields() {
        UUID eventId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-06-13T10:00:00Z");
        Instant publishedAt = Instant.parse("2026-06-13T10:00:01Z");
        Instant ingestedAt = Instant.parse("2026-06-13T10:00:02Z");

        AuditEvent event = AuditEvent.restore(
                eventId,
                AuditEventSource.SOCIAL_SERVICE.value(),
                "FOLLOW_ACCEPTED",
                actorId,
                targetId,
                "trace-2",
                Map.of("followId", "follow-1"),
                occurredAt,
                publishedAt,
                ingestedAt
        );

        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getSource()).isEqualTo("social-service");
        assertThat(event.getType()).isEqualTo("FOLLOW_ACCEPTED");
        assertThat(event.getActorId()).isEqualTo(actorId);
        assertThat(event.getTargetId()).isEqualTo(targetId);
        assertThat(event.getTraceId()).isEqualTo("trace-2");
        assertThat(event.getMetadata()).containsEntry("followId", "follow-1");
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(event.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(event.getIngestedAt()).isEqualTo(ingestedAt);
    }

    @Test
    void ingest_shouldRejectMissingRequiredFields() {
        assertThatThrownBy(() -> AuditEvent.ingest(
                null,
                "auth-service",
                "AUTH_SIGN_IN_SUCCESS",
                null,
                null,
                null,
                Map.of(),
                Instant.now(),
                Instant.now()
        )).isInstanceOf(DomainValidationException.class);

        assertThatThrownBy(() -> AuditEvent.ingest(
                UUID.randomUUID(),
                " ",
                "AUTH_SIGN_IN_SUCCESS",
                null,
                null,
                null,
                Map.of(),
                Instant.now(),
                Instant.now()
        )).isInstanceOf(DomainValidationException.class);
    }
}
