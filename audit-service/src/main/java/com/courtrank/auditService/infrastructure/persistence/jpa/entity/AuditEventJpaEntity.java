package com.courtrank.auditService.infrastructure.persistence.jpa.entity;

import com.courtrank.auditService.domain.entity.AuditEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEventJpaEntity {
    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String type;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "trace_id")
    private String traceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    protected AuditEventJpaEntity() {
    }

    public AuditEventJpaEntity(
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
        this.metadata = metadata;
        this.occurredAt = occurredAt;
        this.publishedAt = publishedAt;
        this.ingestedAt = ingestedAt;
    }

    public static AuditEventJpaEntity fromDomain(AuditEvent event) {
        return new AuditEventJpaEntity(
                event.getEventId(),
                event.getSource(),
                event.getType(),
                event.getActorId(),
                event.getTargetId(),
                event.getTraceId(),
                event.getMetadata(),
                event.getOccurredAt(),
                event.getPublishedAt(),
                event.getIngestedAt()
        );
    }

    public AuditEvent toDomain() {
        return AuditEvent.restore(
                this.eventId,
                this.source,
                this.type,
                this.actorId,
                this.targetId,
                this.traceId,
                this.metadata,
                this.occurredAt,
                this.publishedAt,
                this.ingestedAt
        );
    }
}
