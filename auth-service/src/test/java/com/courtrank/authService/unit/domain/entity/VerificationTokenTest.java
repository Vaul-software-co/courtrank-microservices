package com.courtrank.authService.unit.domain.entity;

import com.courtrank.authService.domain.entity.VerificationToken;
import com.courtrank.authService.domain.enums.VerificationTokenType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class VerificationTokenTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TOKEN_HASH = "token-hash";
    private static final VerificationTokenType TYPE = VerificationTokenType.EMAIL_VERIFICATION;

    private VerificationToken createToken() {
        return VerificationToken.create(
                USER_ID,
                TOKEN_HASH,
                TYPE
        );
    }

    @Test
    void create_shouldCreateValidToken() {
        VerificationToken token = this.createToken();

        assertNotNull(token.getId());
        assertEquals(USER_ID, token.getUserId());
        assertEquals(TOKEN_HASH, token.getTokenHash());
        assertEquals(TYPE, token.getType());
        assertEquals(0, token.getAttempts());
        assertNull(token.getUsedAt());
        assertNotNull(token.getCreatedAt());
        assertNotNull(token.getExpiresAt());
        assertTrue(token.isValid());
    }

    @Test
    void matches_shouldReturnTrueWhenHashMatches() {
        VerificationToken token = this.createToken();

        assertTrue(token.matches(TOKEN_HASH));
    }

    @Test
    void matches_shouldReturnFalseWhenHashDoesNotMatch() {
        VerificationToken token = this.createToken();

        assertFalse(token.matches("other-hash"));
    }

    @Test
    void markAsUsed_shouldInvalidateToken() {
        VerificationToken token = this.createToken();

        token.markAsUsed();

        assertNotNull(token.getUsedAt());
        assertFalse(token.isValid());
    }

    @Test
    void markAsUsed_shouldBeIdempotentWhenAlreadyUsed() {
        VerificationToken token = this.createToken();

        token.markAsUsed();
        Instant usedAt = token.getUsedAt();
        token.markAsUsed();

        assertEquals(usedAt, token.getUsedAt());
    }

    @Test
    void incrementAttempts_shouldIncreaseAttempts() {
        VerificationToken token = this.createToken();

        token.incrementAttempts();

        assertEquals(1, token.getAttempts());
    }

    @Test
    void incrementAttempts_shouldInvalidateTokenWhenMaxAttemptsReached() {
        VerificationToken token = this.createToken();

        token.incrementAttempts();
        token.incrementAttempts();
        token.incrementAttempts();

        assertEquals(3, token.getAttempts());
        assertFalse(token.isValid());
    }

    @Test
    void incrementAttempts_shouldBeIdempotentWhenMaxAttemptsAlreadyReached() {
        VerificationToken token = this.createToken();

        token.incrementAttempts();
        token.incrementAttempts();
        token.incrementAttempts();
        token.incrementAttempts();

        assertEquals(3, token.getAttempts());
    }

    @Test
    void isValid_shouldReturnFalseWhenTokenIsExpired() {
        VerificationToken token = VerificationToken.restore(
                UUID.randomUUID(),
                USER_ID,
                TOKEN_HASH,
                TYPE,
                Instant.now().minusSeconds(1),
                null,
                0,
                Instant.now().minusSeconds(60)
        );

        assertFalse(token.isValid());
    }

    @Test
    void isValid_shouldReturnFalseWhenTokenIsUsed() {
        VerificationToken token = VerificationToken.restore(
                UUID.randomUUID(),
                USER_ID,
                TOKEN_HASH,
                TYPE,
                Instant.now().plusSeconds(3600),
                Instant.now(),
                0,
                Instant.now().minusSeconds(60)
        );

        assertFalse(token.isValid());
    }

    @Test
    void isValid_shouldReturnFalseWhenAttemptsReachedLimit() {
        VerificationToken token = VerificationToken.restore(
                UUID.randomUUID(),
                USER_ID,
                TOKEN_HASH,
                TYPE,
                Instant.now().plusSeconds(3600),
                null,
                3,
                Instant.now().minusSeconds(60)
        );

        assertFalse(token.isValid());
    }

    @Test
    void restore_shouldRehydrateVerificationTokenWithPersistedState() {
        UUID id = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(3600);
        Instant usedAt = Instant.now().minusSeconds(30);
        Instant createdAt = Instant.now().minusSeconds(60);
        int attempts = 2;

        VerificationToken token = VerificationToken.restore(
                id,
                USER_ID,
                TOKEN_HASH,
                TYPE,
                expiresAt,
                usedAt,
                attempts,
                createdAt
        );

        assertEquals(id, token.getId());
        assertEquals(USER_ID, token.getUserId());
        assertEquals(TOKEN_HASH, token.getTokenHash());
        assertEquals(TYPE, token.getType());
        assertEquals(expiresAt, token.getExpiresAt());
        assertEquals(usedAt, token.getUsedAt());
        assertEquals(attempts, token.getAttempts());
        assertEquals(createdAt, token.getCreatedAt());
    }
}
