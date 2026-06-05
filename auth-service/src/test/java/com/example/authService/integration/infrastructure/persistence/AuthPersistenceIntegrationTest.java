package com.example.authService.integration.infrastructure.persistence;

import com.example.authService.domain.entity.Authentication;
import com.example.authService.domain.entity.Session;
import com.example.authService.domain.entity.VerificationToken;
import com.example.authService.domain.enums.SessionStatus;
import com.example.authService.domain.enums.UserRole;
import com.example.authService.domain.enums.VerificationTokenType;
import com.example.authService.domain.exceptions.InvalidCredentialsException;
import com.example.authService.domain.repository.AuthenticationRepository;
import com.example.authService.domain.repository.SessionRepository;
import com.example.authService.domain.repository.VerificationTokenRepository;
import com.example.authService.domain.repository.results.SessionRotationResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "app.password.pepper=test-pepper",
        "app.api-keys.web=web-key",
        "app.api-keys.mobile=mobile-key",
        "app.internal-api-key=internal-key",
        "app.cors.allowed-origins=http://localhost:3000"
})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public class AuthPersistenceIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("courtrank_auth_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    AuthenticationRepository authenticationRepository;

    @Autowired
    SessionRepository sessionRepository;

    @Autowired
    VerificationTokenRepository verificationTokenRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void flyway_shouldApplyAuthSchemaMigrations() {
        Integer migrationCount = this.jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true",
                Integer.class
        );
        Integer authTableCount = this.jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'public' and table_name in ('authentications', 'sessions', 'verification_tokens')",
                Integer.class
        );

        assertEquals(2, migrationCount);
        assertEquals(3, authTableCount);
    }

    @Test
    void authenticationRepository_shouldEnforceCaseInsensitiveEmailUniqueness() {
        this.inTransaction(() -> this.authenticationRepository.save(
                Authentication.create("test@example.com", "hash", UserRole.MEMBER)
        ));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> this.inTransaction(() -> this.authenticationRepository.save(
                        Authentication.create("TEST@example.com", "hash", UserRole.MEMBER)
                ))
        );
    }

    @Test
    void authenticationRepository_shouldSeparateActiveAndDeletedEmailLookups() {
        Authentication auth = Authentication.create("deleted@example.com", "hash", UserRole.MEMBER);
        auth.deleteUser();

        this.inTransaction(() -> this.authenticationRepository.save(auth));

        assertTrue(this.authenticationRepository.findByEmail("deleted@example.com").isEmpty());
        assertTrue(this.authenticationRepository.findByEmailIncludingDeleted("deleted@example.com").isPresent());
    }

    @Test
    void sessionRepository_shouldRotateOnlyActiveNonExpiredSessions() {
        Authentication auth = Authentication.create("rotate@example.com", "hash", UserRole.MEMBER);
        Session oldSession = Session.create(auth.getId(), "old-refresh-hash", "web", "127.0.0.1", "Safari");
        Session newSession = Session.create(auth.getId(), "new-refresh-hash", "web", "127.0.0.1", "Safari");

        this.inTransaction(() -> {
            this.authenticationRepository.save(auth);
            this.sessionRepository.save(oldSession);
        });

        SessionRotationResult result = this.inTransactionResult(() ->
                this.sessionRepository.rotateSession(oldSession.getRefreshTokenHash(), newSession)
        );

        Optional<Session> persistedOldSession = this.sessionRepository.findById(oldSession.getId());
        Optional<Session> persistedNewSession = this.sessionRepository.findById(newSession.getId());

        assertFalse(result.alreadyRevoked());
        assertEquals(SessionStatus.REPLACED, persistedOldSession.orElseThrow().getStatus());
        assertEquals(newSession.getId(), persistedOldSession.orElseThrow().getReplacedBy());
        assertTrue(persistedNewSession.orElseThrow().isActive());
    }

    @Test
    void sessionRepository_shouldNotPersistNewSessionWhenOldSessionIsExpired() {
        Authentication auth = Authentication.create("expired@example.com", "hash", UserRole.MEMBER);
        Session expiredSession = Session.restore(
                UUID.randomUUID(),
                auth.getId(),
                "expired-refresh-hash",
                "web",
                "127.0.0.1",
                "Safari",
                null,
                SessionStatus.ACTIVE,
                null,
                Instant.now().minusSeconds(1),
                Instant.now().minusSeconds(3600)
        );
        Session newSession = Session.create(auth.getId(), "new-expired-refresh-hash", "web", "127.0.0.1", "Safari");

        this.inTransaction(() -> {
            this.authenticationRepository.save(auth);
            this.sessionRepository.save(expiredSession);
        });

        SessionRotationResult result = this.inTransactionResult(() ->
                this.sessionRepository.rotateSession(expiredSession.getRefreshTokenHash(), newSession)
        );

        assertFalse(result.alreadyRevoked());
        assertFalse(result.oldSession().isActive());
        assertTrue(this.sessionRepository.findById(newSession.getId()).isEmpty());
    }

    @Test
    void sessionRepository_shouldRejectSessionForUnknownUser() {
        Session session = Session.create(UUID.randomUUID(), "orphan-refresh-hash", "web", "127.0.0.1", "Safari");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> this.inTransaction(() -> this.sessionRepository.save(session))
        );
    }

    @Test
    void verificationTokenRepository_shouldSupportPasswordResetConfirmationType() {
        Authentication auth = Authentication.create("token@example.com", "hash", UserRole.MEMBER);
        VerificationToken token = VerificationToken.create(
                auth.getId(),
                "reset-confirmation-hash",
                VerificationTokenType.PASSWORD_RESET_CONFIRMATION
        );

        this.inTransaction(() -> {
            this.authenticationRepository.save(auth);
            this.verificationTokenRepository.save(token);
        });

        assertDoesNotThrow(() -> this.verificationTokenRepository.findValid(
                auth.getId(),
                token.getTokenHash(),
                VerificationTokenType.PASSWORD_RESET_CONFIRMATION
        ));
        assertTrue(this.verificationTokenRepository.findValid(
                auth.getId(),
                token.getTokenHash(),
                VerificationTokenType.PASSWORD_RESET_CONFIRMATION
        ).isPresent());
    }

    @Test
    void verificationTokenRepository_shouldRejectInvalidTokenTypeAtDatabaseLevel() {
        Authentication auth = Authentication.create("invalid-token-type@example.com", "hash", UserRole.MEMBER);
        this.inTransaction(() -> this.authenticationRepository.save(auth));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> this.inTransaction(() -> this.jdbcTemplate.update(
                        """
                        insert into verification_tokens (id, user_id, token_hash, type, expires_at, attempts, created_at)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                        UUID.randomUUID(),
                        auth.getId(),
                        "invalid-type-hash",
                        "NOT_A_REAL_TYPE",
                        Instant.now().plusSeconds(300),
                        0,
                        Instant.now()
                ))
        );
    }

    private void inTransaction(Runnable action) {
        new TransactionTemplate(this.transactionManager).executeWithoutResult(status -> action.run());
    }

    private <T> T inTransactionResult(TransactionSupplier<T> supplier) {
        return new TransactionTemplate(this.transactionManager).execute(status -> supplier.get());
    }

    private interface TransactionSupplier<T> {
        T get();
    }
}
