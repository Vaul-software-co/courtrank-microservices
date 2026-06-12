package com.courtrank.socialService.application.dto;

import java.util.UUID;

public record BlockUserRequest(
        UUID blockerId,
        UUID blockedId
) {
}
