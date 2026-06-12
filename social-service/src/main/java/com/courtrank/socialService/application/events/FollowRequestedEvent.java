package com.courtrank.socialService.application.events;

import java.time.Instant;
import java.util.UUID;

public record FollowRequestedEvent(
        UUID followId,
        UUID followerId,
        UUID followingId,
        Instant occurredAt
) {
}
