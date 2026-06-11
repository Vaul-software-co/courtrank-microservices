package com.courtrank.authService.application.events;

import java.time.Instant;
import java.util.UUID;

public record UserEmailVerifiedEvent(
        UUID id,
        String email,
        Instant occurredAt
) {
}
