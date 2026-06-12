package com.courtrank.socialService.application.events;

import java.time.Instant;
import java.util.UUID;

public record FollowRemovedEvent(
        UUID followId,
        UUID followerId,
        UUID followingId,
        UUID removedByUserId,
        Instant occurredAt
) {
}
