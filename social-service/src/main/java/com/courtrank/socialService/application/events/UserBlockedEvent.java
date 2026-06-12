package com.courtrank.socialService.application.events;

import java.time.Instant;
import java.util.UUID;

public record UserBlockedEvent(
        UUID blockId,
        UUID blockerId,
        UUID blockedId,
        Instant occurredAt
) {
}
