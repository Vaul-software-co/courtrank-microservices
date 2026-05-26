package com.example.authService.domain.repository;

import com.example.authService.domain.entity.VerificationToken;
import com.example.authService.domain.enums.VerificationTokenType;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository {
    void save(VerificationToken token);
    void invalidatePrevious(UUID userId, VerificationTokenType type);
    Optional<VerificationToken> findValid(UUID userId, String tokenHash, VerificationTokenType type);
    Optional<VerificationToken> findValid(UUID userId, VerificationTokenType type);
}
