package com.courtrank.authService.domain.repository;

import com.courtrank.authService.domain.entity.VerificationToken;
import com.courtrank.authService.domain.enums.VerificationTokenType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository {
    void save(VerificationToken token);
    void invalidatePrevious(UUID userId, VerificationTokenType type);
    Optional<VerificationToken> findValid(UUID userId, String tokenHash, VerificationTokenType type);
    Optional<VerificationToken> findValid(UUID userId, VerificationTokenType type);
    int deleteConsumedBefore(Instant cutoff);
}
