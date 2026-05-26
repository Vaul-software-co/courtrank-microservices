package com.example.authService.domain.repository.results;

import com.example.authService.domain.entity.Session;

public record SessionRotationResult(
        Session oldSession,
        boolean alreadyRevoked,
        boolean recentRotation
) {
}
