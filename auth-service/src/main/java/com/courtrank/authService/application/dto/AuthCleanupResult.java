package com.courtrank.authService.application.dto;

import java.time.Instant;

public record AuthCleanupResult(
        int deletedSessions,
        int deletedVerificationTokens,
        Instant sessionCutoff,
        Instant verificationTokenCutoff
) {
}
