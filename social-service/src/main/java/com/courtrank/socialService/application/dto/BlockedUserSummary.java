package com.courtrank.socialService.application.dto;

import java.time.Instant;

public record BlockedUserSummary(
        SocialUserSummary user,
        Instant blockedAt
) {
}
