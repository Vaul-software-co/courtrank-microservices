package com.courtrank.socialService.application.dto;

import java.util.UUID;

public record ListFollowingRequest(
        UUID viewerId,
        UUID targetId
) {
}
