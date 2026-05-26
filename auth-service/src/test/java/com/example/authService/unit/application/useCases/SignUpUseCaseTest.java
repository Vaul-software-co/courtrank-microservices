package com.example.authService.unit.application.useCases;

import com.example.authService.application.dto.HttpContext;
import com.example.authService.application.dto.SignUpRequest;
import com.example.authService.application.events.UserRegisteredEvent;
import com.example.authService.application.events.UserRestoredEvent;
import com.example.authService.application.ports.AuthEventPublisher;
import com.example.authService.application.ports.audit.AuditEvent;
import com.example.authService.application.ports.audit.AuditEventType;
import com.example.authService.application.ports.audit.AuditLogger;
import com.example.authService.application.ports.security.PasswordHasher;
import com.example.authService.application.useCases.SignUpUseCase;
import com.example.authService.domain.entity.Authentication;
import com.example.authService.domain.enums.UserRole;
import com.example.authService.domain.exceptions.ConflictException;
import com.example.authService.domain.exceptions.MissedTermsAndConditionsException;
import com.example.authService.domain.exceptions.WeakPasswordException;
import com.example.authService.domain.repository.AuthenticationRepository;
import com.example.authService.domain.service.PasswordPolicy;
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
    PasswordPolicy passwordPolicy;

    @Mock
    AuditLogger auditLogger;

    @InjectMocks
    SignUpUseCase signUpUseCase;

    private static final String NAME = "Test User";
    private static final String USERNAME = "test_user";
    private static final String EMAIL = "test@test.com";
    private static final String PASSWORD = "StrongPass1!";
    private static final String PASSWORD_HASH = "hashed-password";
    private static final String TERMS_VERSION = "v1";

    private final HttpContext http = new HttpContext(
            "web",
            "127.0.0.1",
            "Safari",
            UserRole.MEMBER
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

        this.signUpUseCase.execute(request, this.http);

        ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        verify(this.passwordPolicy).validate(PASSWORD);
        verify(this.authRepository).save(authCaptor.capture());
        verify(this.eventPublisher).publishUserRegistered(eventCaptor.capture());
        verify(this.eventPublisher, never()).publishUserRestored(org.mockito.ArgumentMatchers.any());
        verify(this.auditLogger).log(auditCaptor.capture());

        Authentication savedAuth = authCaptor.getValue();
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
        verifyNoInteractions(this.auditLogger);
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
        verifyNoInteractions(this.auditLogger);
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

        this.signUpUseCase.execute(request, this.http);

        ArgumentCaptor<UserRestoredEvent> eventCaptor = ArgumentCaptor.forClass(UserRestoredEvent.class);
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        assertFalse(existingAuth.isDeleted());
        assertTrue(existingAuth.isActive());
        assertFalse(existingAuth.isEmailVerified());
        assertEquals(PASSWORD_HASH, existingAuth.getPasswordHash());
        assertEquals(TERMS_VERSION, existingAuth.getTermsVersionAccepted());
        assertFalse(existingAuth.isDataAccepted());

        verify(this.authRepository).save(existingAuth);
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
    }
}
