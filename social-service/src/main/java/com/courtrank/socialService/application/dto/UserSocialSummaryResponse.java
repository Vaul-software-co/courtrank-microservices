package com.courtrank.socialService.application.dto;

import com.courtrank.socialService.domain.enums.ViewerFollowStatus;

import java.time.Instant;
import java.util.UUID;

public record UserSocialSummaryResponse(
        UUID userId,
        String name,
        String username,
        String avatarUrl,
        boolean privateProfile,
        ViewerFollowStatus viewerFollowStatus,
        boolean blocked,
        int followersCount,
        int followingCount,
        int pendingRequestsCount,
        int blockedCount,
        Instant createdAt
) {
}
