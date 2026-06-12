package com.courtrank.socialService.application.dto;

import java.util.UUID;

public record UnfollowUserRequest(
        UUID followerId,
        UUID followingId
) {
}
