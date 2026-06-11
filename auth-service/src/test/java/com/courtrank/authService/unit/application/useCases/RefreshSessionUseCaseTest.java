package com.courtrank.authService.unit.application.useCases;

import com.courtrank.authService.application.dto.AuthResponse;
import com.courtrank.authService.application.dto.HttpContext;
import com.courtrank.authService.application.dto.RefreshSessionRequest;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.ports.security.TokenHasher;
import com.courtrank.authService.application.ports.security.TokenService;
import com.courtrank.authService.application.useCases.RefreshSessionUseCase;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.entity.Session;
import com.courtrank.authService.domain.enums.SessionStatus;
import com.courtrank.authService.domain.enums.TokenType;
import com.courtrank.authService.domain.enums.UserRole;
import com.courtrank.authService.domain.exceptions.DisabledAccountException;
import com.courtrank.authService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.domain.repository.SessionRepository;
import com.courtrank.authService.domain.repository.results.SessionRotationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RefreshSessionUseCaseTest {
    @Mock
    TokenService tokenService;

    @Mock
    TokenHasher tokenHasher;

    @Mock
    SessionRepository sessionRepository;

    @Mock
    AuthenticationRepository authenticationRepository;

    @Mock
    AuditLogger auditLogger;

    @InjectMocks
    RefreshSessionUseCase refreshSessionUseCase;

    private static final String EMAIL = "test@test.com";
    private static final String PASSWORD_HASH = "hash";
    private static final String OLD_REFRESH_TOKEN = "old-refresh-token";
    private static final String OLD_REFRESH_TOKEN_HASH = "old-refresh-token-hash";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";
    private static final String NEW_REFRESH_TOKEN_HASH = "new-refresh-token-hash";
    private static final String ACCESS_TOKEN = "access-token";

    private final HttpContext http = new HttpContext(
            "web",
            "127.0.0.1",
            "Safari",
            UserRole.MEMBER
    );

    private Authentication createAuth() {
        Authentication auth = Authentication.create(EMAIL, PASSWORD_HASH, UserRole.MEMBER);
        auth.verifyEmail();
        return auth;
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

    private Session createOldSession(UUID userId) {
        return Session.create(userId, OLD_REFRESH_TOKEN_HASH, "web", "127.0.0.1", "Safari");
    }

    @Test
    void execute_shouldRotateRefreshSessionAndReturnNewTokens() {
        Authentication auth = this.createAuth();
        Session oldSession = this.createOldSession(auth.getId());

        when(this.tokenService.verifyRefresh(OLD_REFRESH_TOKEN))
                .thenReturn(true);
        when(this.tokenService.getTokenId(OLD_REFRESH_TOKEN))
                .thenReturn(auth.getId());
        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));
        when(this.tokenHasher.hash(OLD_REFRESH_TOKEN))
                .thenReturn(OLD_REFRESH_TOKEN_HASH);
        when(this.tokenService.generateToken(auth.getId(), TokenType.REFRESH))
                .thenReturn(NEW_REFRESH_TOKEN);
        when(this.tokenHasher.hash(NEW_REFRESH_TOKEN))
                .thenReturn(NEW_REFRESH_TOKEN_HASH);
        when(this.sessionRepository.rotateSession(org.mockito.ArgumentMatchers.eq(OLD_REFRESH_TOKEN_HASH), org.mockito.ArgumentMatchers.any(Session.class)))
                .thenReturn(new SessionRotationResult(oldSession, false, false));
        when(this.tokenService.generateAccessToken(eq(auth.getId()), any(UUID.class), eq(auth.getRole())))
                .thenReturn(ACCESS_TOKEN);

        AuthResponse response = this.refreshSessionUseCase.execute(
                new RefreshSessionRequest(OLD_REFRESH_TOKEN, this.http)
        );

        ArgumentCaptor<Session> newSessionCaptor = ArgumentCaptor.forClass(Session.class);
        verify(this.sessionRepository).rotateSession(
                org.mockito.ArgumentMatchers.eq(OLD_REFRESH_TOKEN_HASH),
                newSessionCaptor.capture()
        );

        Session newSession = newSessionCaptor.getValue();
        assertEquals(ACCESS_TOKEN, response.accessToken());
        assertEquals(NEW_REFRESH_TOKEN, response.refreshToken());
        assertTrue(response.clubId().isEmpty());
        assertEquals(auth.getId(), newSession.getUserId());
        assertEquals(NEW_REFRESH_TOKEN_HASH, newSession.getRefreshTokenHash());
        assertEquals(this.http.client(), newSession.getClient());
        assertEquals(this.http.ip(), newSession.getIp());
        assertEquals(this.http.userAgent(), newSession.getUserAgent());
        assertTrue(newSession.isActive());
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenRefreshTokenIsInvalid() {
        when(this.tokenService.verifyRefresh(OLD_REFRESH_TOKEN))
                .thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.refreshSessionUseCase.execute(new RefreshSessionRequest(OLD_REFRESH_TOKEN, this.http))
        );

        verify(this.tokenService).verifyRefresh(OLD_REFRESH_TOKEN);
        verify(this.tokenService, never()).getTokenId(OLD_REFRESH_TOKEN);
        verifyNoInteractions(this.authenticationRepository);
        verifyNoInteractions(this.tokenHasher);
        verifyNoInteractions(this.sessionRepository);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();

        when(this.tokenService.verifyRefresh(OLD_REFRESH_TOKEN))
                .thenReturn(true);
        when(this.tokenService.getTokenId(OLD_REFRESH_TOKEN))
                .thenReturn(userId);
        when(this.authenticationRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.refreshSessionUseCase.execute(new RefreshSessionRequest(OLD_REFRESH_TOKEN, this.http))
        );

        verifyNoInteractions(this.tokenHasher);
        verifyNoInteractions(this.sessionRepository);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserIsDeleted() {
        Authentication auth = this.createAuth();
        auth.deleteUser();

        when(this.tokenService.verifyRefresh(OLD_REFRESH_TOKEN))
                .thenReturn(true);
        when(this.tokenService.getTokenId(OLD_REFRESH_TOKEN))
                .thenReturn(auth.getId());
        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.refreshSessionUseCase.execute(new RefreshSessionRequest(OLD_REFRESH_TOKEN, this.http))
        );

        verifyNoInteractions(this.tokenHasher);
        verifyNoInteractions(this.sessionRepository);
    }

    @Test
    void execute_shouldThrowDisabledAccountWhenUserIsInactive() {
        Authentication auth = this.createInactiveAuth();

        when(this.tokenService.verifyRefresh(OLD_REFRESH_TOKEN))
                .thenReturn(true);
        when(this.tokenService.getTokenId(OLD_REFRESH_TOKEN))
                .thenReturn(auth.getId());
        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));

        assertThrows(
                DisabledAccountException.class,
                () -> this.refreshSessionUseCase.execute(new RefreshSessionRequest(OLD_REFRESH_TOKEN, this.http))
        );

        verifyNoInteractions(this.tokenHasher);
        verifyNoInteractions(this.sessionRepository);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenOldSessionIsExpired() {
        Authentication auth = this.createAuth();
        Session expiredOldSession = Session.restore(
                UUID.randomUUID(),
                auth.getId(),
                OLD_REFRESH_TOKEN_HASH,
                "web",
                "127.0.0.1",
                "Safari",
                null,
                SessionStatus.ACTIVE,
                null,
                Instant.now().minusSeconds(1),
                Instant.now().minusSeconds(3600)
        );

        when(this.tokenService.verifyRefresh(OLD_REFRESH_TOKEN))
                .thenReturn(true);
        when(this.tokenService.getTokenId(OLD_REFRESH_TOKEN))
                .thenReturn(auth.getId());
        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));
        when(this.tokenHasher.hash(OLD_REFRESH_TOKEN))
                .thenReturn(OLD_REFRESH_TOKEN_HASH);
        when(this.tokenService.generateToken(auth.getId(), TokenType.REFRESH))
                .thenReturn(NEW_REFRESH_TOKEN);
        when(this.tokenHasher.hash(NEW_REFRESH_TOKEN))
                .thenReturn(NEW_REFRESH_TOKEN_HASH);
        when(this.sessionRepository.rotateSession(org.mockito.ArgumentMatchers.eq(OLD_REFRESH_TOKEN_HASH), org.mockito.ArgumentMatchers.any(Session.class)))
                .thenReturn(new SessionRotationResult(expiredOldSession, false, false));

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.refreshSessionUseCase.execute(new RefreshSessionRequest(OLD_REFRESH_TOKEN, this.http))
        );

        verify(this.tokenService, never()).generateAccessToken(eq(auth.getId()), any(UUID.class), eq(auth.getRole()));
    }

    @Test
    void execute_shouldRevokeAllSessionsAndThrowInvalidCredentialsWhenRevokedTokenReuseIsNotRecent() {
        Authentication auth = this.createAuth();
        Session oldSession = this.createOldSession(auth.getId());
        oldSession.revoke();

        when(this.tokenService.verifyRefresh(OLD_REFRESH_TOKEN))
                .thenReturn(true);
        when(this.tokenService.getTokenId(OLD_REFRESH_TOKEN))
                .thenReturn(auth.getId());
        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));
        when(this.tokenHasher.hash(OLD_REFRESH_TOKEN))
                .thenReturn(OLD_REFRESH_TOKEN_HASH);
        when(this.tokenService.generateToken(auth.getId(), TokenType.REFRESH))
                .thenReturn(NEW_REFRESH_TOKEN);
        when(this.tokenHasher.hash(NEW_REFRESH_TOKEN))
                .thenReturn(NEW_REFRESH_TOKEN_HASH);
        when(this.sessionRepository.rotateSession(org.mockito.ArgumentMatchers.eq(OLD_REFRESH_TOKEN_HASH), org.mockito.ArgumentMatchers.any(Session.class)))
                .thenReturn(new SessionRotationResult(oldSession, true, false));

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.refreshSessionUseCase.execute(new RefreshSessionRequest(OLD_REFRESH_TOKEN, this.http))
        );

        verify(this.sessionRepository).revokeAllByUserId(auth.getId());
        verify(this.tokenService, never()).generateAccessToken(eq(auth.getId()), any(UUID.class), eq(auth.getRole()));
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWithoutRevokingAllWhenRevokedTokenReuseIsRecent() {
        Authentication auth = this.createAuth();
        Session oldSession = this.createOldSession(auth.getId());
        oldSession.revoke();

        when(this.tokenService.verifyRefresh(OLD_REFRESH_TOKEN))
                .thenReturn(true);
        when(this.tokenService.getTokenId(OLD_REFRESH_TOKEN))
                .thenReturn(auth.getId());
        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));
        when(this.tokenHasher.hash(OLD_REFRESH_TOKEN))
                .thenReturn(OLD_REFRESH_TOKEN_HASH);
        when(this.tokenService.generateToken(auth.getId(), TokenType.REFRESH))
                .thenReturn(NEW_REFRESH_TOKEN);
        when(this.tokenHasher.hash(NEW_REFRESH_TOKEN))
                .thenReturn(NEW_REFRESH_TOKEN_HASH);
        when(this.sessionRepository.rotateSession(org.mockito.ArgumentMatchers.eq(OLD_REFRESH_TOKEN_HASH), org.mockito.ArgumentMatchers.any(Session.class)))
                .thenReturn(new SessionRotationResult(oldSession, true, true));

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.refreshSessionUseCase.execute(new RefreshSessionRequest(OLD_REFRESH_TOKEN, this.http))
        );

        verify(this.sessionRepository, never()).revokeAllByUserId(auth.getId());
        verify(this.tokenService, never()).generateAccessToken(eq(auth.getId()), any(UUID.class), eq(auth.getRole()));
    }
}
