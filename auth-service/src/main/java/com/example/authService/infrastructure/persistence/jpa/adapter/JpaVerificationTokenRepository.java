package com.example.authService.infrastructure.persistence.jpa.adapter;

import com.example.authService.domain.entity.VerificationToken;
import com.example.authService.domain.enums.VerificationTokenType;
import com.example.authService.domain.repository.VerificationTokenRepository;
import com.example.authService.infrastructure.persistence.jpa.entity.VerificationTokenJpaEntity;
import com.example.authService.infrastructure.persistence.jpa.repository.SpringVerificationTokenJpaRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaVerificationTokenRepository implements VerificationTokenRepository {
    private final SpringVerificationTokenJpaRepository repository;

    public JpaVerificationTokenRepository(SpringVerificationTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(VerificationToken token) {
        this.repository.save(VerificationTokenJpaEntity.fromDomain(token));
    }

    @Override
    @Transactional
    public void invalidatePrevious(UUID userId, VerificationTokenType type) {
        this.repository.markUnusedAsUsed(userId, type, Instant.now());
    }

    @Override
    public Optional<VerificationToken> findValid(UUID userId, String tokenHash, VerificationTokenType type) {
        return this.repository.findFirstByUserIdAndTokenHashAndTypeAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                        userId,
                        tokenHash,
                        type,
                        Instant.now()
                )
                .map(VerificationTokenJpaEntity::toDomain)
                .filter(VerificationToken::isValid);
    }

    @Override
    public Optional<VerificationToken> findValid(UUID userId, VerificationTokenType type) {
        return this.repository.findFirstByUserIdAndTypeAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                        userId,
                        type,
                        Instant.now()
                )
                .map(VerificationTokenJpaEntity::toDomain)
                .filter(VerificationToken::isValid);
    }
}
