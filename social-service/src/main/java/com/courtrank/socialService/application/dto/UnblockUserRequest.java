package com.courtrank.socialService.application.dto;

import java.util.UUID;

public record UnblockUserRequest(
        UUID blockerId,
        UUID blockedId
) {
}
