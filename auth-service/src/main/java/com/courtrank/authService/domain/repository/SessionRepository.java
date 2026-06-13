package com.courtrank.authService.domain.repository;

import com.courtrank.authService.domain.entity.Session;
import com.courtrank.authService.domain.repository.results.SessionRotationResult;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

public interface SessionRepository {
    void save(Session session);
    Optional<Session> findById(UUID id);
    Optional<Session> findByTokenHash(String tokenHash);
    List<Session> findActiveByUserId(UUID userId);
    SessionRotationResult rotateSession(String oldTokenHash, Session newSession);
    void revokeAllByUserId(UUID userId);
    int deleteInactiveBefore(Instant cutoff);
}
