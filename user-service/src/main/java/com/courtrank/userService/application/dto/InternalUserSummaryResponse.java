package com.courtrank.userService.application.dto;

import com.courtrank.userService.domain.enums.UserProfileStatus;

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
