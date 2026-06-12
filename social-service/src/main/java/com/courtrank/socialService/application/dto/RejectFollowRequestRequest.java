package com.courtrank.socialService.application.dto;

import java.util.UUID;

public record RejectFollowRequestRequest(
        UUID ownerId,
        UUID followId
) {
}
