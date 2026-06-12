package com.courtrank.socialService.application.dto;

import java.util.UUID;

public record AreUsersBlockedRequest(
        UUID userA,
        UUID userB
) {
}
