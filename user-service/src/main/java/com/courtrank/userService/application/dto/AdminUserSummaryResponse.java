package com.courtrank.userService.application.dto;

import com.courtrank.userService.domain.enums.UserProfileStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminUserSummaryResponse(
        UUID id,
        String name,
        String username,
        String email,
        boolean emailVerified,
        String phoneNumber,
        String avatarUrl,
        boolean privateProfile,
        UserProfileStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
