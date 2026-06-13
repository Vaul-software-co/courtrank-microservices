package com.courtrank.authService.application.useCases;

import com.courtrank.authService.application.dto.AuthCleanupResult;
import com.courtrank.authService.domain.repository.SessionRepository;
import com.courtrank.authService.domain.repository.VerificationTokenRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class CleanupAuthArtifactsUseCase {
    private final SessionRepository sessionRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final Clock clock;

    public CleanupAuthArtifactsUseCase(
            SessionRepository sessionRepository,
            VerificationTokenRepository verificationTokenRepository,
            Clock clock
    ) {
        this.sessionRepository = sessionRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.clock = clock;
    }

    public AuthCleanupResult execute(Duration sessionRetention, Duration verificationTokenRetention) {
        Instant now = this.clock.instant();
        Instant sessionCutoff = now.minus(sessionRetention);
        Instant verificationTokenCutoff = now.minus(verificationTokenRetention);

        int deletedVerificationTokens = this.verificationTokenRepository.deleteConsumedBefore(verificationTokenCutoff);
        int deletedSessions = this.sessionRepository.deleteInactiveBefore(sessionCutoff);

        return new AuthCleanupResult(
                deletedSessions,
                deletedVerificationTokens,
                sessionCutoff,
                verificationTokenCutoff
        );
    }
}
