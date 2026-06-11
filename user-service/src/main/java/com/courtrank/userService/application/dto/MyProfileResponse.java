package com.courtrank.userService.application.dto;

import com.courtrank.userService.domain.enums.UserGender;
import com.courtrank.userService.domain.enums.UserProfileStatus;

import java.time.Instant;
import java.util.UUID;

public record MyProfileResponse(
        UUID id,
        String name,
        String username,
        String email,
        boolean isEmailVerified,
        String phoneNumber,
        UserGender gender,
        String avatarUrl,
        boolean privateProfile,
        UserProfileStatus status,
        String lang,
        Instant createdAt
) {
}
