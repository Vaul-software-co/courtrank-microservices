package com.example.authService.unit.application.useCases;

import com.example.authService.application.dto.ListSessionsRequest;
import com.example.authService.application.dto.SessionSummary;
import com.example.authService.application.ports.audit.AuditLogger;
import com.example.authService.application.useCases.ListSessionsUseCase;
import com.example.authService.domain.entity.Authentication;
import com.example.authService.domain.entity.Session;
import com.example.authService.domain.enums.SessionStatus;
import com.example.authService.domain.enums.UserRole;
import com.example.authService.domain.exceptions.InvalidCredentialsException;
import com.example.authService.domain.repository.AuthenticationRepository;
import com.example.authService.domain.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListSessionsUseCaseTest {
    @Mock
    AuthenticationRepository authenticationRepository;

    @Mock
    SessionRepository sessionRepository;

    @Mock
    AuditLogger auditLogger;

    @InjectMocks
    ListSessionsUseCase listSessionsUseCase;

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

    private Session createSession(UUID userId, String hash, String client) {
        return Session.create(userId, hash, client, "127.0.0.1", "Safari");
    }

    @Test
    void execute_shouldReturnActiveSessionSummaries() {
        Authentication auth = this.createAuth();
        Session activeSession = this.createSession(auth.getId(), "hash-1", "web");
        Session expiredSession = Session.restore(
                UUID.randomUUID(),
                auth.getId(),
                "hash-2",
                "mobile",
                "127.0.0.1",
                "Safari",
                null,
                SessionStatus.ACTIVE,
                null,
                Instant.now().minusSeconds(1),
                Instant.now().minusSeconds(3600)
        );

        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));
        when(this.sessionRepository.findActiveByUserId(auth.getId()))
                .thenReturn(List.of(activeSession, expiredSession));

        List<SessionSummary> sessions = this.listSessionsUseCase.execute(new ListSessionsRequest(auth.getId()));

        assertEquals(1, sessions.size());
        assertEquals(activeSession.getId(), sessions.get(0).id());
        assertEquals(activeSession.getClient(), sessions.get(0).client());
        assertEquals(activeSession.getIp(), sessions.get(0).ip());
        assertEquals(activeSession.getUserAgent(), sessions.get(0).userAgent());
        assertEquals(activeSession.getCreatedAt(), sessions.get(0).createdAt());
        assertEquals(activeSession.getExpiresAt(), sessions.get(0).expiresAt());
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();

        when(this.authenticationRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.listSessionsUseCase.execute(new ListSessionsRequest(userId))
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
                () -> this.listSessionsUseCase.execute(new ListSessionsRequest(auth.getId()))
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
                () -> this.listSessionsUseCase.execute(new ListSessionsRequest(auth.getId()))
        );

        verifyNoInteractions(this.sessionRepository);
    }
}
