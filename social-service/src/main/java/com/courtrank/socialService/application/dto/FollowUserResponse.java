package com.courtrank.socialService.application.dto;

import com.courtrank.socialService.domain.enums.FollowStatus;

import java.util.UUID;

public record FollowUserResponse(
        UUID followId,
        FollowStatus status
) {
}
