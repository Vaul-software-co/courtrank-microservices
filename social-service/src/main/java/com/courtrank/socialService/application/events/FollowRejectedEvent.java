package com.courtrank.socialService.application.events;

import java.time.Instant;
import java.util.UUID;

public record FollowRejectedEvent(
        UUID followId,
        UUID followerId,
        UUID followingId,
        Instant occurredAt
) {
}
