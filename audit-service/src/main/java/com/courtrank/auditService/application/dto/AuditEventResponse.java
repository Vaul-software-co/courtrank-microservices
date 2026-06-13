package com.courtrank.auditService.application.dto;

import com.courtrank.auditService.domain.entity.AuditEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(
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
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
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
}
