package com.courtrank.socialService.application.dto;

import java.time.Instant;
import java.util.UUID;

public record FollowRequestSummary(
        UUID followId,
        SocialUserSummary user,
        Instant requestedAt
) {
}
