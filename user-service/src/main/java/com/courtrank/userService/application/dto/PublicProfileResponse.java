package com.courtrank.userService.application.dto;

import com.courtrank.userService.domain.enums.UserProfileStatus;

import java.time.Instant;
import java.util.UUID;

public record PublicProfileResponse(
        UUID id,
        String name,
        String username,
        String avatarUrl,
        boolean privateProfile,
        UserProfileStatus status,
        Instant createdAt
) {
}
