package com.courtrank.auditService.domain.entity;

import com.courtrank.auditService.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class AuditEvent {
    private UUID eventId;
    private String source;
    private String type;
    private UUID actorId;
    private UUID targetId;
    private String traceId;
    private Map<String, Object> metadata;
    private Instant occurredAt;
    private Instant publishedAt;
    private Instant ingestedAt;

    private AuditEvent(
            UUID eventId,
            String source,
            String type,
            UUID actorId,
            UUID targetId,
            String traceId,
            Map<String, Object> metadata,
            Instant occurredAt,
            Instant publishedAt,
            Instant ingestedAt
    ) {
        this.eventId = eventId;
        this.source = source;
        this.type = type;
        this.actorId = actorId;
        this.targetId = targetId;
        this.traceId = traceId;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        this.occurredAt = occurredAt;
        this.publishedAt = publishedAt;
        this.ingestedAt = ingestedAt;
    }

    public static AuditEvent ingest(
            UUID eventId,
            String source,
            String type,
            UUID actorId,
            UUID targetId,
            String traceId,
            Map<String, Object> metadata,
            Instant occurredAt,
            Instant publishedAt
    ) {
        Instant now = Instant.now();
        AuditEvent event = new AuditEvent(
                eventId,
                source,
                type,
                actorId,
                targetId,
                traceId,
                metadata,
                occurredAt,
                publishedAt,
                now
        );
        event.validate();
        return event;
    }

    public static AuditEvent restore(
            UUID eventId,
            String source,
            String type,
            UUID actorId,
            UUID targetId,
            String traceId,
            Map<String, Object> metadata,
            Instant occurredAt,
            Instant publishedAt,
            Instant ingestedAt
    ) {
        AuditEvent event = new AuditEvent(
                eventId,
                source,
                type,
                actorId,
                targetId,
                traceId,
                metadata,
                occurredAt,
                publishedAt,
                ingestedAt
        );
        event.validate();
        return event;
    }

    private void validate() {
        if (this.eventId == null) {
            throw new DomainValidationException("Audit event id is required");
        }
        if (isBlank(this.source)) {
            throw new DomainValidationException("Audit event source is required");
        }
        if (isBlank(this.type)) {
            throw new DomainValidationException("Audit event type is required");
        }
        if (this.occurredAt == null) {
            throw new DomainValidationException("Audit event occurredAt is required");
        }
        if (this.publishedAt == null) {
            throw new DomainValidationException("Audit event publishedAt is required");
        }
        if (this.ingestedAt == null) {
            throw new DomainValidationException("Audit event ingestedAt is required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getSource() {
        return source;
    }

    public String getType() {
        return type;
    }

    public UUID getActorId() {
        return actorId;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTraceId() {
        return traceId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }
}
