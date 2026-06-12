package com.courtrank.userService.application.events;

import java.time.Instant;
import java.util.UUID;

public record UserProfileDeletedEvent(
        UUID id,
        Instant occurredAt
) {
}
