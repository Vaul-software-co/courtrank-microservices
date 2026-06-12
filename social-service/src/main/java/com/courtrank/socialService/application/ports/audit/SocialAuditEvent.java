package com.courtrank.socialService.application.ports.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SocialAuditEvent(
        SocialAuditEventType type,
        UUID actorId,
        UUID targetId,
        String traceId,
        Map<String, Object> metadata,
        Instant occurredAt
) {
}
