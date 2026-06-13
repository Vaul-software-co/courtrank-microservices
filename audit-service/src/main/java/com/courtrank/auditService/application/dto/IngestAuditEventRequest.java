package com.courtrank.auditService.application.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record IngestAuditEventRequest(
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
}
