package com.courtrank.authService.application.events;

import java.time.Instant;
import java.util.UUID;

public record UserDeletedEvent(
        UUID id,
        String email,
        Instant occurredAt
) {
}
