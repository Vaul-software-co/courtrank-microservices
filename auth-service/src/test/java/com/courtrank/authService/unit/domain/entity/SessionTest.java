package com.courtrank.authService.unit.domain.entity;

import com.courtrank.authService.domain.entity.Session;
import com.courtrank.authService.domain.enums.SessionStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SessionTest {
    private static final String REFRESH_TOKEN_HASH = "refresh-token-hash";
    private static final String CLIENT = "web";
    private static final String IP = "127.0.0.1";
    private static final String USER_AGENT = "Safari";
    private static final UUID USER_ID = UUID.randomUUID();

    private Session createSession() {
        return Session.create(
                USER_ID,
                REFRESH_TOKEN_HASH,
                CLIENT,
                IP,
                USER_AGENT
        );
    }

    @Test
    void create_shouldCreateActiveSession(){
        Session session = this.createSession();

        assertNotNull(session.getId());
        assertEquals(USER_ID, session.getUserId());
        assertEquals(REFRESH_TOKEN_HASH, session.getRefreshTokenHash());
        assertEquals(CLIENT, session.getClient());
        assertEquals(IP, session.getIp());
        assertEquals(USER_AGENT, session.getUserAgent());
        assertEquals(SessionStatus.ACTIVE, session.getStatus());
        assertNull(session.getRevokedAt());
        assertNull(session.getReplacedBy());
        assertNotNull(session.getCreatedAt());
        assertNotNull(session.getExpiresAt());
        assertTrue(session.isActive());
    }

    @Test
    void revoke_shouldMarkSessionAsRevoked(){
        Session session = this.createSession();

        session.revoke();
        assertEquals(SessionStatus.REVOKED, session.getStatus());
        assertNotNull(session.getRevokedAt());
        assertFalse(session.isActive());
    }

    @Test
    void revoke_shouldBeIdempotentWhenAlreadyRevoked(){
        Session session = this.createSession();

        session.revoke();
        Instant revokedAt = session.getRevokedAt();
        session.revoke();
        assertEquals(revokedAt, session.getRevokedAt());
        assertEquals(SessionStatus.REVOKED, session.getStatus());
    }

    @Test
    void isActive_shouldReturnFalseWhenSessionIsRevoked(){
        Session session = this.createSession();

        session.revoke();
        assertFalse(session.isActive());
    }

    @Test
    void isActive_shouldReturnFalseWhenSessionIsExpired(){
        Session session = Session.restore(
                UUID.randomUUID(),
                USER_ID,
                REFRESH_TOKEN_HASH,
                CLIENT,
                IP,
                USER_AGENT,
                null,
                SessionStatus.ACTIVE,
                null,
                Instant.now().minusSeconds(1),
                Instant.now().minusSeconds(2)
        );

        assertFalse(session.isActive());
    }

    @Test
    void isActive_shouldReturnFalseWhenSessionWasReplaced() {
        UUID replacedBy = UUID.randomUUID();

        Session session = Session.restore(
                UUID.randomUUID(),
                USER_ID,
                REFRESH_TOKEN_HASH,
                CLIENT,
                IP,
                USER_AGENT,
                replacedBy,
                SessionStatus.REPLACED,
                null,
                Instant.now().plusSeconds(3600),
                Instant.now()
        );

        assertFalse(session.isActive());
        assertEquals(SessionStatus.REPLACED, session.getStatus());
        assertEquals(replacedBy, session.getReplacedBy());
    }

    @Test
    void restore_shouldRehydrateSessionWithPersistedState() {
        UUID id = UUID.randomUUID();
        UUID replacedBy = UUID.randomUUID();
        Instant revokedAt = Instant.now().minusSeconds(60);
        Instant expiresAt = Instant.now().plusSeconds(3600);
        Instant createdAt = Instant.now().minusSeconds(120);

        Session session = Session.restore(
                id,
                USER_ID,
                REFRESH_TOKEN_HASH,
                CLIENT,
                IP,
                USER_AGENT,
                replacedBy,
                SessionStatus.REPLACED,
                revokedAt,
                expiresAt,
                createdAt
        );

        assertEquals(id, session.getId());
        assertEquals(USER_ID, session.getUserId());
        assertEquals(REFRESH_TOKEN_HASH, session.getRefreshTokenHash());
        assertEquals(CLIENT, session.getClient());
        assertEquals(IP, session.getIp());
        assertEquals(USER_AGENT, session.getUserAgent());
        assertEquals(replacedBy, session.getReplacedBy());
        assertEquals(SessionStatus.REPLACED, session.getStatus());
        assertEquals(revokedAt, session.getRevokedAt());
        assertEquals(expiresAt, session.getExpiresAt());
        assertEquals(createdAt, session.getCreatedAt());
    }
}
