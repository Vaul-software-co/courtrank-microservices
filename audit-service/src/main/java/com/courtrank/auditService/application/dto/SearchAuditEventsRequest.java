package com.courtrank.auditService.application.dto;

import java.time.Instant;
import java.util.UUID;

public record SearchAuditEventsRequest(
        String source,
        String type,
        UUID userId,
        UUID actorId,
        UUID targetId,
        String traceId,
        Instant occurredFrom,
        Instant occurredTo,
        int limit,
        int offset
) {
}
