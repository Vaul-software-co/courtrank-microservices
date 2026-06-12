package com.courtrank.socialService.application.dto;

import java.util.UUID;

public record FollowUserRequest(
        UUID followerId,
        UUID followingId
) {
}