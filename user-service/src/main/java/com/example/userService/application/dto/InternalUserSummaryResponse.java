package com.example.userService.application.dto;

import com.example.userService.domain.enums.UserProfileStatus;

import java.util.UUID;

public record InternalUserSummaryResponse(
        UUID id,
        String name,
        String username,
        String email,
        String avatarUrl,
        boolean privateProfile,
        UserProfileStatus status
) {
}
