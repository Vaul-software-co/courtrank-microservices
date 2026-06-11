package com.courtrank.authService.domain.repository.results;

import com.courtrank.authService.domain.entity.Session;

public record SessionRotationResult(
        Session oldSession,
        boolean alreadyRevoked,
        boolean recentRotation
) {
}
