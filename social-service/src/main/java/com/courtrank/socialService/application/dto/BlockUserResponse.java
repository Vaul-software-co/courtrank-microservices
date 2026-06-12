package com.courtrank.socialService.application.dto;

import java.util.UUID;

public record BlockUserResponse(
        UUID blockId,
        boolean created
) {
}
