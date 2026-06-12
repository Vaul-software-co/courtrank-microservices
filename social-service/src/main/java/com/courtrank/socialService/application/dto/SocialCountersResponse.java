package com.courtrank.socialService.application.dto;

import java.util.UUID;

public record SocialCountersResponse(
        UUID userId,
        int followersCount,
        int followingCount,
        int pendingRequestsCount,
        int blockedCount
) {
}
