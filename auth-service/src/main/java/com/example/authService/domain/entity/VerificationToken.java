package com.example.authService.domain.entity;

import com.example.authService.domain.enums.VerificationTokenType;

import java.time.Instant;
import java.util.UUID;

public class VerificationToken {
    private UUID id;
    private UUID userId;
    private String tokenHash;
    private VerificationTokenType type;
    private Instant expiresAt;
    private Instant usedAt;
    private int attempts;
    private Instant createdAt;
    private static final int MAX_ATTEMPTS = 3;

    private VerificationToken(
            UUID id,
            UUID userId,
            String tokenHash,
            VerificationTokenType type,
            Instant expiresAt,
            Instant usedAt,
            int attempts,
            Instant createdAt
    ){
      this.id = id;
      this.userId = userId;
      this.tokenHash = tokenHash;
      this.type = type;
      this.expiresAt = expiresAt;
      this.usedAt = usedAt;
      this.attempts = attempts;
      this.createdAt = createdAt;
    }

    public static VerificationToken create (
            UUID userId,
            String tokenHash,
            VerificationTokenType type
    ){
        Instant now = Instant.now();
        Instant expires = now.plus(type.getExpiration());
        return new VerificationToken(
                UUID.randomUUID(),
                userId,
                tokenHash,
                type,
                expires,
                null,
                0,
                now
        );
    }

    public static VerificationToken restore(
            UUID id,
            UUID userId,
            String tokenHash,
            VerificationTokenType type,
            Instant expiresAt,
            Instant usedAt,
            int attempts,
            Instant createdAt
    ){
        return new VerificationToken(
                id,
                userId,
                tokenHash,
                type,
                expiresAt,
                usedAt,
                attempts,
                createdAt
        );
    }

    public boolean isValid() {
        return this.usedAt == null
                && this.expiresAt.isAfter(Instant.now())
                && this.attempts < MAX_ATTEMPTS;
    }

    public UUID getId() {
        return this.id;
    }

    public UUID getUserId() {
        return this.userId;
    }

    public String getTokenHash() {
        return this.tokenHash;
    }

    public VerificationTokenType getType() {
        return this.type;
    }

    public Instant getExpiresAt() {
        return this.expiresAt;
    }

    public Instant getUsedAt() {
        return this.usedAt;
    }

    public int getAttempts() {
        return this.attempts;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public boolean matches(String tokenHash) {
        return this.tokenHash.equals(tokenHash);
    }

    public void markAsUsed() {
        if (this.usedAt != null) return;
        this.usedAt = Instant.now();
    }

    public void incrementAttempts() {
        if(this.attempts >= MAX_ATTEMPTS) return;
        this.attempts++;
    }
}
