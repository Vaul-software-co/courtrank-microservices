package com.example.authService.unit.application.useCases;

import com.example.authService.application.dto.AuthResponse;
import com.example.authService.application.dto.HttpContext;
import com.example.authService.application.dto.SignInRequest;
import com.example.authService.application.ports.authorization.WorkerAccess;
import com.example.authService.application.ports.authorization.WorkerAccessVerifier;
import com.example.authService.application.ports.audit.AuditLogger;
import com.example.authService.application.ports.security.PasswordHasher;
import com.example.authService.application.ports.security.TokenHasher;
import com.example.authService.application.ports.security.TokenService;
import com.example.authService.application.useCases.SignInUseCase;
import com.example.authService.domain.entity.Authentication;
import com.example.authService.domain.entity.Session;
import com.example.authService.domain.enums.TokenType;
import com.example.authService.domain.enums.UserRole;
import com.example.authService.domain.exceptions.DisabledAccountException;
import com.example.authService.domain.exceptions.EmailNotVerifiedException;
import com.example.authService.domain.exceptions.ForbiddenException;
import com.example.authService.domain.exceptions.InvalidCredentialsException;
import com.example.authService.domain.repository.AuthenticationRepository;
import com.example.authService.domain.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SignInUseCaseTest {
    @Mock
    AuthenticationRepository authenticationRepository;

    @Mock
    PasswordHasher passwordHasher;

    @Mock
    SessionRepository sessionRepository;

    @Mock
    TokenService tokenService;

    @Mock
    TokenHasher tokenHasher;

    @Mock
    WorkerAccessVerifier workerAccess;

    @Mock
    AuditLogger auditLogger;

    @InjectMocks
    SignInUseCase signInUseCase;

    private static final String EMAIL = "test@test.com";
    private static final String PASSWORD = "StrongPass1!";
    private static final String PASSWORD_HASH = "hash";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String REFRESH_TOKEN_HASH = "refresh-token-hash";

    private final HttpContext memberHttp = new HttpContext(
            "mobile",
            "127.0.0.1",
            "Safari",
            UserRole.MEMBER
    );

    private final HttpContext adminHttp = new HttpContext(
            "web",
            "127.0.0.1",
            "Safari",
            UserRole.ADMIN
    );

    private Authentication createVerifiedAuth(UserRole role) {
        Authentication auth = Authentication.create(EMAIL, PASSWORD_HASH, role);
        auth.verifyEmail();
        return auth;
    }

    private Authentication createInactiveAuth() {
        Authentication auth = this.createVerifiedAuth(UserRole.MEMBER);

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

    @Test
    void execute_shouldReturnTokensAndSaveSessionWhenCredentialsAreValid() {
        Authentication auth = this.createVerifiedAuth(UserRole.MEMBER);

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(auth));
        when(this.passwordHasher.checkPassword(PASSWORD, PASSWORD_HASH))
                .thenReturn(true);
        when(this.tokenService.generateToken(auth.getId(), TokenType.REFRESH))
                .thenReturn(REFRESH_TOKEN);
        when(this.tokenHasher.hash(REFRESH_TOKEN))
                .thenReturn(REFRESH_TOKEN_HASH);
        when(this.tokenService.generateAccessToken(eq(auth.getId()), any(UUID.class), eq(auth.getRole())))
                .thenReturn(ACCESS_TOKEN);

        AuthResponse response = this.signInUseCase.execute(new SignInRequest(EMAIL, PASSWORD), this.memberHttp);

        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
        verify(this.sessionRepository).save(sessionCaptor.capture());

        Session savedSession = sessionCaptor.getValue();
        assertEquals(ACCESS_TOKEN, response.accessToken());
        assertEquals(REFRESH_TOKEN, response.refreshToken());
        assertTrue(response.clubId().isEmpty());
        assertEquals(auth.getId(), savedSession.getUserId());
        assertEquals(REFRESH_TOKEN_HASH, savedSession.getRefreshTokenHash());
        assertEquals(this.memberHttp.client(), savedSession.getClient());
        assertEquals(this.memberHttp.ip(), savedSession.getIp());
        assertEquals(this.memberHttp.userAgent(), savedSession.getUserAgent());
        assertTrue(savedSession.isActive());
        verifyNoInteractions(this.workerAccess);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenEmailDoesNotExist() {
        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.signInUseCase.execute(new SignInRequest(EMAIL, PASSWORD), this.memberHttp)
        );

        verifyNoInteractions(this.passwordHasher);
        verifyNoInteractions(this.tokenService);
        verifyNoInteractions(this.tokenHasher);
        verifyNoInteractions(this.sessionRepository);
        verifyNoInteractions(this.workerAccess);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserIsDeleted() {
        Authentication auth = this.createVerifiedAuth(UserRole.MEMBER);
        auth.deleteUser();

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(auth));
        when(this.passwordHasher.checkPassword(PASSWORD, PASSWORD_HASH))
                .thenReturn(true);

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.signInUseCase.execute(new SignInRequest(EMAIL, PASSWORD), this.memberHttp)
        );

        verifyNoInteractions(this.tokenService);
        verifyNoInteractions(this.tokenHasher);
        verifyNoInteractions(this.sessionRepository);
        verifyNoInteractions(this.workerAccess);
    }

    @Test
    void execute_shouldThrowDisabledAccountWhenUserIsInactive() {
        Authentication auth = this.createInactiveAuth();

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(auth));
        when(this.passwordHasher.checkPassword(PASSWORD, PASSWORD_HASH))
                .thenReturn(true);

        assertThrows(
                DisabledAccountException.class,
                () -> this.signInUseCase.execute(new SignInRequest(EMAIL, PASSWORD), this.memberHttp)
        );

        verifyNoInteractions(this.tokenService);
        verifyNoInteractions(this.tokenHasher);
        verifyNoInteractions(this.sessionRepository);
        verifyNoInteractions(this.workerAccess);
    }

    @Test
    void execute_shouldThrowEmailNotVerifiedWhenEmailIsNotVerified() {
        Authentication auth = Authentication.create(EMAIL, PASSWORD_HASH, UserRole.MEMBER);

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(auth));
        when(this.passwordHasher.checkPassword(PASSWORD, PASSWORD_HASH))
                .thenReturn(true);

        assertThrows(
                EmailNotVerifiedException.class,
                () -> this.signInUseCase.execute(new SignInRequest(EMAIL, PASSWORD), this.memberHttp)
        );

        verifyNoInteractions(this.tokenService);
        verifyNoInteractions(this.tokenHasher);
        verifyNoInteractions(this.sessionRepository);
        verifyNoInteractions(this.workerAccess);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenPasswordIsWrong() {
        Authentication auth = this.createVerifiedAuth(UserRole.MEMBER);

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(auth));
        when(this.passwordHasher.checkPassword("WrongPass1!", PASSWORD_HASH))
                .thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.signInUseCase.execute(new SignInRequest(EMAIL, "WrongPass1!"), this.memberHttp)
        );

        verifyNoInteractions(this.tokenService);
        verifyNoInteractions(this.tokenHasher);
        verifyNoInteractions(this.sessionRepository);
        verifyNoInteractions(this.workerAccess);
    }

    @Test
    void execute_shouldVerifyWorkerAccessAndReturnClubIdWhenMemberSignsInAsAdminContext() {
        Authentication auth = this.createVerifiedAuth(UserRole.MEMBER);
        UUID clubId = UUID.randomUUID();

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(auth));
        when(this.passwordHasher.checkPassword(PASSWORD, PASSWORD_HASH))
                .thenReturn(true);
        when(this.workerAccess.verify(auth.getId()))
                .thenReturn(new WorkerAccess(true, clubId));
        when(this.tokenService.generateToken(auth.getId(), TokenType.REFRESH))
                .thenReturn(REFRESH_TOKEN);
        when(this.tokenHasher.hash(REFRESH_TOKEN))
                .thenReturn(REFRESH_TOKEN_HASH);
        when(this.tokenService.generateAccessToken(eq(auth.getId()), any(UUID.class), eq(auth.getRole())))
                .thenReturn(ACCESS_TOKEN);

        AuthResponse response = this.signInUseCase.execute(new SignInRequest(EMAIL, PASSWORD), this.adminHttp);

        assertEquals(Optional.of(clubId), response.clubId());
        verify(this.workerAccess).verify(auth.getId());
        verify(this.sessionRepository).save(org.mockito.ArgumentMatchers.any(Session.class));
    }

    @Test
    void execute_shouldThrowForbiddenWhenWorkerAccessIsDenied() {
        Authentication auth = this.createVerifiedAuth(UserRole.MEMBER);

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(auth));
        when(this.passwordHasher.checkPassword(PASSWORD, PASSWORD_HASH))
                .thenReturn(true);
        when(this.workerAccess.verify(auth.getId()))
                .thenReturn(new WorkerAccess(false, null));

        assertThrows(
                ForbiddenException.class,
                () -> this.signInUseCase.execute(new SignInRequest(EMAIL, PASSWORD), this.adminHttp)
        );

        verify(this.workerAccess).verify(auth.getId());
        verifyNoInteractions(this.tokenService);
        verifyNoInteractions(this.tokenHasher);
        verifyNoInteractions(this.sessionRepository);
    }

    @Test
    void execute_shouldNotVerifyWorkerAccessWhenUserRoleIsAdmin() {
        Authentication auth = this.createVerifiedAuth(UserRole.ADMIN);

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(auth));
        when(this.passwordHasher.checkPassword(PASSWORD, PASSWORD_HASH))
                .thenReturn(true);
        when(this.tokenService.generateToken(auth.getId(), TokenType.REFRESH))
                .thenReturn(REFRESH_TOKEN);
        when(this.tokenHasher.hash(REFRESH_TOKEN))
                .thenReturn(REFRESH_TOKEN_HASH);
        when(this.tokenService.generateAccessToken(eq(auth.getId()), any(UUID.class), eq(auth.getRole())))
                .thenReturn(ACCESS_TOKEN);

        AuthResponse response = this.signInUseCase.execute(new SignInRequest(EMAIL, PASSWORD), this.adminHttp);

        assertTrue(response.clubId().isEmpty());
        verifyNoInteractions(this.workerAccess);
        verify(this.sessionRepository).save(org.mockito.ArgumentMatchers.any(Session.class));
    }
}
