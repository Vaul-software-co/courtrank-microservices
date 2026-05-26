package com.example.authService.unit.application.useCases;

import com.example.authService.application.dto.RevokeSessionRequest;
import com.example.authService.application.ports.audit.AuditLogger;
import com.example.authService.application.useCases.RevokeSessionUseCase;
import com.example.authService.domain.entity.Authentication;
import com.example.authService.domain.entity.Session;
import com.example.authService.domain.enums.SessionStatus;
import com.example.authService.domain.enums.UserRole;
import com.example.authService.domain.exceptions.ForbiddenException;
import com.example.authService.domain.exceptions.InvalidCredentialsException;
import com.example.authService.domain.repository.AuthenticationRepository;
import com.example.authService.domain.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RevokeSessionUseCaseTest {
    @Mock
    AuthenticationRepository authenticationRepository;

    @Mock
    SessionRepository sessionRepository;

    @Mock
    AuditLogger auditLogger;

    @InjectMocks
    RevokeSessionUseCase revokeSessionUseCase;

    private static final String EMAIL = "test@test.com";
    private static final String PASSWORD_HASH = "hash";

    private Authentication createAuth() {
        return Authentication.create(EMAIL, PASSWORD_HASH, UserRole.MEMBER);
    }

    private Authentication createInactiveAuth() {
        Authentication auth = this.createAuth();

        return Authentication.restore(
                auth.getId(),
                auth.getEmail(),
                auth.getPasswordHash(),
                auth.getRole(),
                auth.isEmailVerified(),
                false,
                auth.getTermsVersionAccepted(),
                auth.getTermsAcceptedAt(),
                null,
                auth.getDeletedAt(),
                auth.getCreatedAt(),
                auth.getUpdatedAt()
        );
    }

    private Session createSession(UUID userId) {
        return Session.create(userId, "hash", "web", "127.0.0.1", "Safari");
    }

    @Test
    void execute_shouldRevokeOwnActiveSession() {
        Authentication auth = this.createAuth();
        Session session = this.createSession(auth.getId());

        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));
        when(this.sessionRepository.findById(session.getId()))
                .thenReturn(Optional.of(session));

        this.revokeSessionUseCase.execute(new RevokeSessionRequest(auth.getId(), session.getId()));

        assertEquals(SessionStatus.REVOKED, session.getStatus());
        assertNotNull(session.getRevokedAt());
        assertFalse(session.isActive());
        verify(this.sessionRepository).save(session);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(this.authenticationRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.revokeSessionUseCase.execute(new RevokeSessionRequest(userId, sessionId))
        );

        verifyNoInteractions(this.sessionRepository);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserIsDeleted() {
        Authentication auth = this.createAuth();
        auth.deleteUser();

        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.revokeSessionUseCase.execute(new RevokeSessionRequest(auth.getId(), UUID.randomUUID()))
        );

        verifyNoInteractions(this.sessionRepository);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserIsInactive() {
        Authentication auth = this.createInactiveAuth();

        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.revokeSessionUseCase.execute(new RevokeSessionRequest(auth.getId(), UUID.randomUUID()))
        );

        verifyNoInteractions(this.sessionRepository);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenSessionDoesNotExist() {
        Authentication auth = this.createAuth();
        UUID sessionId = UUID.randomUUID();

        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));
        when(this.sessionRepository.findById(sessionId))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.revokeSessionUseCase.execute(new RevokeSessionRequest(auth.getId(), sessionId))
        );

        verify(this.sessionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void execute_shouldThrowForbiddenWhenSessionBelongsToAnotherUser() {
        Authentication auth = this.createAuth();
        Session session = this.createSession(UUID.randomUUID());

        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));
        when(this.sessionRepository.findById(session.getId()))
                .thenReturn(Optional.of(session));

        assertThrows(
                ForbiddenException.class,
                () -> this.revokeSessionUseCase.execute(new RevokeSessionRequest(auth.getId(), session.getId()))
        );

        verify(this.sessionRepository, never()).save(session);
    }

    @Test
    void execute_shouldDoNothingWhenSessionIsNotActive() {
        Authentication auth = this.createAuth();
        Session session = Session.restore(
                UUID.randomUUID(),
                auth.getId(),
                "hash",
                "web",
                "127.0.0.1",
                "Safari",
                null,
                SessionStatus.REVOKED,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Instant.now().minusSeconds(60)
        );

        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));
        when(this.sessionRepository.findById(session.getId()))
                .thenReturn(Optional.of(session));

        this.revokeSessionUseCase.execute(new RevokeSessionRequest(auth.getId(), session.getId()));

        verify(this.sessionRepository, never()).save(session);
    }
}
