package com.example.authService.infrastructure.persistence.jpa.adapter;

import com.example.authService.domain.entity.Session;
import com.example.authService.domain.enums.SessionStatus;
import com.example.authService.domain.exceptions.InvalidCredentialsException;
import com.example.authService.domain.repository.SessionRepository;
import com.example.authService.domain.repository.results.SessionRotationResult;
import com.example.authService.infrastructure.persistence.jpa.entity.SessionJpaEntity;
import com.example.authService.infrastructure.persistence.jpa.repository.SpringSessionJpaRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaSessionRepository implements SessionRepository {
    private static final Duration RECENT_ROTATION_WINDOW = Duration.ofSeconds(10);

    private final SpringSessionJpaRepository repository;

    public JpaSessionRepository(SpringSessionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(Session session) {
        this.repository.save(SessionJpaEntity.fromDomain(session));
    }

    @Override
    public Optional<Session> findById(UUID id) {
        return this.repository.findById(id)
                .map(SessionJpaEntity::toDomain);
    }

    @Override
    public Optional<Session> findByTokenHash(String tokenHash) {
        return this.repository.findByRefreshTokenHash(tokenHash)
                .map(SessionJpaEntity::toDomain);
    }

    @Override
    public List<Session> findActiveByUserId(UUID userId) {
        return this.repository.findByUserIdAndStatus(userId, SessionStatus.ACTIVE)
                .stream()
                .map(SessionJpaEntity::toDomain)
                .filter(Session::isActive)
                .toList();
    }

    @Override
    @Transactional
    public SessionRotationResult rotateSession(String oldTokenHash, Session newSession) {
        SessionJpaEntity oldSessionEntity = this.repository.findByRefreshTokenHashForUpdate(oldTokenHash)
                .orElseThrow(InvalidCredentialsException::new);
        Session oldSession = oldSessionEntity.toDomain();

        if (oldSessionEntity.getStatus() != SessionStatus.ACTIVE) {
            return new SessionRotationResult(
                    oldSession,
                    true,
                    this.isRecentRotation(oldSessionEntity.getRevokedAt())
            );
        }

        if (!oldSession.isActive()) {
            return new SessionRotationResult(oldSession, false, false);
        }

        this.repository.save(SessionJpaEntity.fromDomain(newSession));
        int replacedRows = this.repository.markActiveSessionAsReplaced(
                oldTokenHash,
                newSession.getId(),
                Instant.now()
        );

        if (replacedRows == 0) {
            throw new InvalidCredentialsException();
        }

        return new SessionRotationResult(oldSession, false, false);
    }

    @Override
    @Transactional
    public void revokeAllByUserId(UUID userId) {
        this.repository.revokeActiveByUserId(userId, Instant.now());
    }

    private boolean isRecentRotation(Instant revokedAt) {
        return revokedAt != null && revokedAt.isAfter(Instant.now().minus(RECENT_ROTATION_WINDOW));
    }
}
