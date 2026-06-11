package com.courtrank.authService.application.ports.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEvent(
        AuditEventType type,
        UUID actorId,
        UUID targetId,
        String traceId,
        Map<String, Object> metadata,
        Instant occurredAt
) {
}
