package com.courtrank.socialService.application.dto;

import java.util.UUID;

public record AcceptFollowRequestRequest(
        UUID ownerId,
        UUID followId
) {
}
