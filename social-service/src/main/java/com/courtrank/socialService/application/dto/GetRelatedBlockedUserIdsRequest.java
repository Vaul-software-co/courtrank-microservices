package com.courtrank.socialService.application.dto;

import java.util.UUID;

public record GetRelatedBlockedUserIdsRequest(
        UUID userId
) {
}
