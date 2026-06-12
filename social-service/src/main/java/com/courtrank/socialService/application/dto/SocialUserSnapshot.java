package com.courtrank.socialService.application.dto;

import java.time.Instant;
import java.util.UUID;

public record SocialUserSnapshot(
        UUID userId,
        String name,
        String username,
        String avatarUrl,
        boolean privateProfile,
        boolean active,
        Instant deletedAt,
        Instant sourceUpdatedAt
) {
}
