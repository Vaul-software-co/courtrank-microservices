package com.example.authService.infrastructure.persistence.jpa.entity;

import com.example.authService.domain.entity.VerificationToken;
import com.example.authService.domain.enums.VerificationTokenType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_tokens")
public class VerificationTokenJpaEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationTokenType type;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected VerificationTokenJpaEntity() {
    }

    public VerificationTokenJpaEntity(
            UUID id,
            UUID userId,
            String tokenHash,
            VerificationTokenType type,
            Instant expiresAt,
            Instant usedAt,
            int attempts,
            Instant createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.type = type;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.attempts = attempts;
        this.createdAt = createdAt;
    }

    public static VerificationTokenJpaEntity fromDomain(VerificationToken token) {
        return new VerificationTokenJpaEntity(
                token.getId(),
                token.getUserId(),
                token.getTokenHash(),
                token.getType(),
                token.getExpiresAt(),
                token.getUsedAt(),
                token.getAttempts(),
                token.getCreatedAt()
        );
    }

    public VerificationToken toDomain() {
        return VerificationToken.restore(
                this.id,
                this.userId,
                this.tokenHash,
                this.type,
                this.expiresAt,
                this.usedAt,
                this.attempts,
                this.createdAt
        );
    }
}
