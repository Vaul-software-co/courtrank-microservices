package com.courtrank.socialService.application.dto;

import java.util.UUID;

public record SearchSocialUsersRequest(
        UUID viewerId,
        String query,
        int limit
) {
}
