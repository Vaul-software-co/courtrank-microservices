package com.example.userService.application.dto;

import com.example.userService.domain.enums.UserProfileStatus;

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
