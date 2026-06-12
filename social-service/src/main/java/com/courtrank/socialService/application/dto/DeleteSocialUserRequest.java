package com.courtrank.socialService.application.dto;

import java.time.Instant;
import java.util.UUID;

public record DeleteSocialUserRequest(
        UUID userId,
        Instant deletedAt,
        Instant sourceUpdatedAt
) {
}
