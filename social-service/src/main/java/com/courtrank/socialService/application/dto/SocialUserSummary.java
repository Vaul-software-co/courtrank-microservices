package com.courtrank.socialService.application.dto;

import java.time.Instant;
import java.util.UUID;

public record SocialUserSummary(
        UUID userId,
        String name,
        String username,
        String avatarUrl,
        Instant relatedAt
) {
}
