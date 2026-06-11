package com.courtrank.authService.unit.application.useCases;

import com.courtrank.authService.application.dto.AuthResponse;
import com.courtrank.authService.application.dto.HttpContext;
import com.courtrank.authService.application.dto.SignUpRequest;
import com.courtrank.authService.application.dto.SignUpResponse;
import com.courtrank.authService.application.events.UserRegisteredEvent;
import com.courtrank.authService.application.events.UserRestoredEvent;
import com.courtrank.authService.application.ports.AuthEventPublisher;
import com.courtrank.authService.application.ports.audit.AuditEvent;
import com.courtrank.authService.application.ports.audit.AuditEventType;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.ports.security.PasswordHasher;
import com.courtrank.authService.application.ports.user.UsernameAvailabilityVerifier;
import com.courtrank.authService.application.services.SessionIssuer;
import com.courtrank.authService.application.useCases.SignUpUseCase;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.enums.UserRole;
import com.courtrank.authService.domain.exceptions.ConflictException;
import com.courtrank.authService.domain.exceptions.MissedTermsAndConditionsException;
import com.courtrank.authService.domain.exceptions.WeakPasswordException;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.domain.service.PasswordPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SignUpUseCaseTest {
    @Mock
    AuthenticationRepository authRepository;

    @Mock
    PasswordHasher passwordHasher;

    @Mock
    AuthEventPublisher eventPublisher;

    @Mock
    UsernameAvailabilityVerifier usernameAvailabilityVerifier;

    @Mock
    PasswordPolicy passwordPolicy;

    @Mock
    AuditLogger auditLogger;

    @Mock
    SessionIssuer sessionIssuer;

    @InjectMocks
    SignUpUseCase signUpUseCase;

    private static final String NAME = "Test User";
    private static final String USERNAME = "test_user";
    private static final String EMAIL = "test@test.com";
    private static final String PASSWORD = "StrongPass1!";
    private static final String PASSWORD_HASH = "hashed-password";
    private static final String TERMS_VERSION = "v1";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";

    private final HttpContext http = new HttpContext(
            "web",
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

    private SignUpRequest createRequest(boolean terms, boolean commercial) {
        return new SignUpRequest(
                NAME,
                USERNAME,
                EMAIL,
                PASSWORD,
                terms,
                TERMS_VERSION,
                commercial
        );
    }

    @Test
    void execute_shouldRegisterNewUserAndPublishRegisteredEvent() {
        SignUpRequest request = this.createRequest(true, true);

        when(this.passwordHasher.hashPassword(PASSWORD))
                .thenReturn(PASSWORD_HASH);
        when(this.authRepository.findByEmailIncludingDeleted(EMAIL))
                .thenReturn(Optional.empty());

        ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        when(this.sessionIssuer.issue(org.mockito.ArgumentMatchers.any(Authentication.class), org.mockito.ArgumentMatchers.eq(this.http)))
                .thenReturn(new AuthResponse(ACCESS_TOKEN, REFRESH_TOKEN, Optional.empty()));

        SignUpResponse response = this.signUpUseCase.execute(request, this.http);

        verify(this.passwordPolicy).validate(PASSWORD);
        verify(this.authRepository).save(authCaptor.capture());
        verify(this.eventPublisher).publishUserRegistered(eventCaptor.capture());
        verify(this.eventPublisher, never()).publishUserRestored(org.mockito.ArgumentMatchers.any());
        verify(this.auditLogger).log(auditCaptor.capture());

        Authentication savedAuth = authCaptor.getValue();
        verify(this.usernameAvailabilityVerifier).assertAvailable(USERNAME, savedAuth.getId());
        assertEquals(EMAIL, savedAuth.getEmail());
        assertEquals(PASSWORD_HASH, savedAuth.getPasswordHash());
        assertEquals(UserRole.MEMBER, savedAuth.getRole());
        assertEquals(TERMS_VERSION, savedAuth.getTermsVersionAccepted());
        assertNotNull(savedAuth.getTermsAcceptedAt());
        assertTrue(savedAuth.isDataAccepted());
        assertFalse(savedAuth.isDeleted());

        UserRegisteredEvent event = eventCaptor.getValue();
        assertEquals(savedAuth.getId(), event.id());
        assertEquals(EMAIL, event.email());
        assertEquals(NAME, event.name());
        assertEquals(USERNAME, event.username());
        assertEquals(UserRole.MEMBER, event.role());
        assertEquals(TERMS_VERSION, event.acceptedTermsVersion());
        assertTrue(event.acceptedDataCommercialization());
        assertNotNull(event.occurredAt());

        AuditEvent auditEvent = auditCaptor.getValue();
        assertEquals(AuditEventType.AUTH_SIGN_UP_SUCCESS, auditEvent.type());
        assertEquals(savedAuth.getId(), auditEvent.actorId());
        assertEquals(savedAuth.getId(), auditEvent.targetId());
        assertEquals("web", auditEvent.metadata().get("client"));
        assertEquals(savedAuth, response.authentication());
        assertTrue(response.auth().isPresent());
        assertEquals(ACCESS_TOKEN, response.auth().orElseThrow().accessToken());
        assertEquals(REFRESH_TOKEN, response.auth().orElseThrow().refreshToken());
        verify(this.sessionIssuer).issue(savedAuth, this.http);
    }

    @Test
    void execute_shouldThrowMissedTermsAndConditionsWhenTermsAreNotAccepted() {
        SignUpRequest request = this.createRequest(false, false);

        assertThrows(
                MissedTermsAndConditionsException.class,
                () -> this.signUpUseCase.execute(request, this.http)
        );

        verifyNoInteractions(this.passwordPolicy);
        verifyNoInteractions(this.passwordHasher);
        verifyNoInteractions(this.authRepository);
        verifyNoInteractions(this.eventPublisher);
        verifyNoInteractions(this.usernameAvailabilityVerifier);
        verifyNoInteractions(this.auditLogger);
        verifyNoInteractions(this.sessionIssuer);
    }

    @Test
    void execute_shouldThrowWeakPasswordWhenPasswordPolicyRejectsPassword() {
        SignUpRequest request = this.createRequest(true, false);

        doThrow(new WeakPasswordException("weak password"))
                .when(this.passwordPolicy)
                .validate(PASSWORD);

        assertThrows(
                WeakPasswordException.class,
                () -> this.signUpUseCase.execute(request, this.http)
        );

        verify(this.passwordPolicy).validate(PASSWORD);
        verifyNoInteractions(this.passwordHasher);
        verifyNoInteractions(this.authRepository);
        verifyNoInteractions(this.eventPublisher);
        verifyNoInteractions(this.usernameAvailabilityVerifier);
        verifyNoInteractions(this.auditLogger);
        verifyNoInteractions(this.sessionIssuer);
    }

    @Test
    void execute_shouldThrowConflictWhenEmailAlreadyExistsAndIsNotDeleted() {
        SignUpRequest request = this.createRequest(true, false);
        Authentication existingAuth = Authentication.create(EMAIL, PASSWORD_HASH, UserRole.MEMBER);

        when(this.passwordHasher.hashPassword(PASSWORD))
                .thenReturn(PASSWORD_HASH);
        when(this.authRepository.findByEmailIncludingDeleted(EMAIL))
                .thenReturn(Optional.of(existingAuth));

        assertThrows(
                ConflictException.class,
                () -> this.signUpUseCase.execute(request, this.http)
        );

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        verify(this.passwordPolicy).validate(PASSWORD);
        verify(this.passwordHasher).hashPassword(PASSWORD);
        verify(this.authRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(this.eventPublisher);
        verifyNoInteractions(this.usernameAvailabilityVerifier);
        verifyNoInteractions(this.sessionIssuer);
        verify(this.auditLogger).log(auditCaptor.capture());

        AuditEvent auditEvent = auditCaptor.getValue();
        assertEquals(AuditEventType.AUTH_SIGN_UP_CONFLICT, auditEvent.type());
        assertEquals(existingAuth.getId(), auditEvent.targetId());
        assertEquals("web", auditEvent.metadata().get("client"));
    }

    @Test
    void execute_shouldRestoreDeletedUserAndPublishRestoredEvent() {
        SignUpRequest request = this.createRequest(true, false);
        Authentication existingAuth = Authentication.create(EMAIL, "old-hash", UserRole.MEMBER);
        existingAuth.deleteUser();

        when(this.passwordHasher.hashPassword(PASSWORD))
                .thenReturn(PASSWORD_HASH);
        when(this.authRepository.findByEmailIncludingDeleted(EMAIL))
                .thenReturn(Optional.of(existingAuth));

        ArgumentCaptor<UserRestoredEvent> eventCaptor = ArgumentCaptor.forClass(UserRestoredEvent.class);
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        when(this.sessionIssuer.issue(existingAuth, this.http))
                .thenReturn(new AuthResponse(ACCESS_TOKEN, REFRESH_TOKEN, Optional.empty()));

        SignUpResponse response = this.signUpUseCase.execute(request, this.http);

        assertFalse(existingAuth.isDeleted());
        assertTrue(existingAuth.isActive());
        assertFalse(existingAuth.isEmailVerified());
        assertEquals(PASSWORD_HASH, existingAuth.getPasswordHash());
        assertEquals(TERMS_VERSION, existingAuth.getTermsVersionAccepted());
        assertFalse(existingAuth.isDataAccepted());

        verify(this.authRepository).save(existingAuth);
        verify(this.usernameAvailabilityVerifier).assertAvailable(USERNAME, existingAuth.getId());
        verify(this.eventPublisher).publishUserRestored(eventCaptor.capture());
        verify(this.eventPublisher, never()).publishUserRegistered(org.mockito.ArgumentMatchers.any());
        verify(this.auditLogger).log(auditCaptor.capture());

        UserRestoredEvent event = eventCaptor.getValue();
        assertEquals(existingAuth.getId(), event.id());
        assertEquals(EMAIL, event.email());
        assertEquals(NAME, event.name());
        assertEquals(USERNAME, event.username());
        assertEquals(UserRole.MEMBER, event.role());
        assertEquals(TERMS_VERSION, event.acceptedTermsVersion());
        assertFalse(event.acceptedDataCommercialization());
        assertNotNull(event.occurredAt());

        AuditEvent auditEvent = auditCaptor.getValue();
        assertEquals(AuditEventType.AUTH_SIGN_UP_RESTORED_USER, auditEvent.type());
        assertEquals(existingAuth.getId(), auditEvent.actorId());
        assertEquals(existingAuth.getId(), auditEvent.targetId());
        assertEquals(existingAuth, response.authentication());
        assertTrue(response.auth().isPresent());
        verify(this.sessionIssuer).issue(existingAuth, this.http);
    }

    @Test
    void execute_shouldNotIssueSessionWhenRegisteredUserIsNotMember() {
        SignUpRequest request = this.createRequest(true, false);

        when(this.passwordHasher.hashPassword(PASSWORD))
                .thenReturn(PASSWORD_HASH);
        when(this.authRepository.findByEmailIncludingDeleted(EMAIL))
                .thenReturn(Optional.empty());

        SignUpResponse response = this.signUpUseCase.execute(request, this.adminHttp);

        ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(this.authRepository).save(authCaptor.capture());

        Authentication savedAuth = authCaptor.getValue();
        assertEquals(UserRole.ADMIN, savedAuth.getRole());
        assertEquals(savedAuth, response.authentication());
        assertTrue(response.auth().isEmpty());
        verifyNoInteractions(this.sessionIssuer);
    }
}
