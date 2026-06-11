package com.courtrank.authService.infrastructure.persistence.jpa.entity;

import com.courtrank.authService.domain.entity.Session;
import com.courtrank.authService.domain.enums.SessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class SessionJpaEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "refresh_token_hash", nullable = false)
    private String refreshTokenHash;

    @Column(nullable = false)
    private String client;

    @Column
    private String ip;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SessionJpaEntity() {
    }

    public SessionJpaEntity(
            UUID id,
            UUID userId,
            String refreshTokenHash,
            String client,
            String ip,
            String userAgent,
            UUID replacedBy,
            SessionStatus status,
            Instant revokedAt,
            Instant expiresAt,
            Instant createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.refreshTokenHash = refreshTokenHash;
        this.client = client;
        this.ip = ip;
        this.userAgent = userAgent;
        this.replacedBy = replacedBy;
        this.status = status;
        this.revokedAt = revokedAt;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static SessionJpaEntity fromDomain(Session session) {
        return new SessionJpaEntity(
                session.getId(),
                session.getUserId(),
                session.getRefreshTokenHash(),
                session.getClient(),
                session.getIp(),
                session.getUserAgent(),
                session.getReplacedBy(),
                session.getStatus(),
                session.getRevokedAt(),
                session.getExpiresAt(),
                session.getCreatedAt()
        );
    }

    public Session toDomain() {
        return Session.restore(
                this.id,
                this.userId,
                this.refreshTokenHash,
                this.client,
                this.ip,
                this.userAgent,
                this.replacedBy,
                this.status,
                this.revokedAt,
                this.expiresAt,
                this.createdAt
        );
    }

    public UUID getUserId() {
        return this.userId;
    }

    public SessionStatus getStatus() {
        return this.status;
    }

    public Instant getRevokedAt() {
        return this.revokedAt;
    }
}
