package com.courtrank.authService.domain.entity;

import com.courtrank.authService.domain.enums.SessionStatus;

import java.time.Instant;
import java.util.UUID;

public class Session {
    private UUID id;
    private UUID userId;
    private String refreshTokenHash;
    private String client;
    private String ip;
    private String userAgent;
    private UUID replacedBy;
    private SessionStatus status;
    private Instant revokedAt;
    private Instant expiresAt;
    private Instant createdAt;
    private static final long DEFAULT_SESSION_DURATION_SECONDS = 60 * 60 * 24 * 7;

    private Session(
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

    public static Session create(
            UUID userId,
            String refreshTokenHash,
            String client,
            String ip,
            String userAgent
    ) {
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(DEFAULT_SESSION_DURATION_SECONDS);
        return new Session(
                UUID.randomUUID(),
                userId,
                refreshTokenHash,
                client,
                ip,
                userAgent,
                null,
                SessionStatus.ACTIVE,
                null,
                expires,
                now
        );
    }

    public static Session restore(
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
        return new Session(
                id,
                userId,
                refreshTokenHash,
                client,
                ip,
                userAgent,
                replacedBy,
                status,
                revokedAt,
                expiresAt,
                createdAt
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public String getClient() {
        return client;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public UUID getReplacedBy() {
        return replacedBy;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void revoke(){
        if (this.status == SessionStatus.REVOKED) return;

        this.status = SessionStatus.REVOKED;
        this.revokedAt = Instant.now();
    }

    public boolean isActive() {
        return status == SessionStatus.ACTIVE && expiresAt.isAfter(Instant.now());
    }
}
