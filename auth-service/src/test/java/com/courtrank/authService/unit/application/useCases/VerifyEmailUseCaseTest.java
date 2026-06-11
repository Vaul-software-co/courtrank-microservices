package com.courtrank.authService.unit.application.useCases;

import com.courtrank.authService.application.dto.VerifyEmailRequest;
import com.courtrank.authService.application.events.UserEmailVerifiedEvent;
import com.courtrank.authService.application.ports.AuthEventPublisher;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.ports.security.PasswordHasher;
import com.courtrank.authService.application.ports.security.VerificationTokenGenerator;
import com.courtrank.authService.application.useCases.VerifyEmailUseCase;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.entity.VerificationToken;
import com.courtrank.authService.domain.enums.UserRole;
import com.courtrank.authService.domain.enums.VerificationTokenType;
import com.courtrank.authService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.domain.repository.VerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VerifyEmailUseCaseTest {
    @Mock
    VerificationTokenRepository tokenRepository;

    @Mock
    VerificationTokenGenerator tokenGenerator;

    @Mock
    AuthenticationRepository authenticationRepository;

    @Mock
    PasswordHasher passwordHasher;

    @Mock
    AuditLogger auditLogger;

    @Mock
    AuthEventPublisher eventPublisher;

    @InjectMocks
    VerifyEmailUseCase verifyEmailUseCase;

    private static final String EMAIL = "test@test.com";
    private static final String PASSWORD_HASH = "hash";
    private static final String PASSWORD = "OldPass1!";
    private static final UserRole ROLE = UserRole.MEMBER;
    private static final String RAW_TOKEN = "raw-token";
    private static final String TOKEN_HASH = "token-hash";

    private Authentication createAuth() {
        return Authentication.create(
                EMAIL,
                PASSWORD_HASH,
                ROLE
        );
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

    private VerificationToken createEmailVerificationToken(Authentication auth) {
        return VerificationToken.create(
                auth.getId(),
                TOKEN_HASH,
                VerificationTokenType.EMAIL_VERIFICATION
        );
    }

    @Test
    void execute_shouldVerifyEmailAndMarkTokenAsUsedWhenTokenAndPasswordAreValid() {
        Authentication auth = this.createAuth();
        VerificationToken token = this.createEmailVerificationToken(auth);

        when(this.tokenGenerator.hash(RAW_TOKEN))
                .thenReturn(TOKEN_HASH);
        when(this.tokenRepository.findValid(auth.getId(), TOKEN_HASH, VerificationTokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(token));
        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));
        when(this.passwordHasher.checkPassword(PASSWORD, auth.getPasswordHash()))
                .thenReturn(true);

        this.verifyEmailUseCase.execute(new VerifyEmailRequest(auth.getId(), RAW_TOKEN, PASSWORD));

        ArgumentCaptor<UserEmailVerifiedEvent> eventCaptor = ArgumentCaptor.forClass(UserEmailVerifiedEvent.class);
        assertTrue(auth.isEmailVerified());
        assertFalse(token.isValid());
        assertEquals(1, token.getAttempts());
        verify(this.authenticationRepository).save(auth);
        verify(this.tokenRepository).save(token);
        verify(this.eventPublisher).publishUserEmailVerified(eventCaptor.capture());
        assertEquals(auth.getId(), eventCaptor.getValue().id());
        assertEquals(auth.getEmail(), eventCaptor.getValue().email());
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenTokenDoesNotExist() {
        Authentication auth = this.createAuth();

        when(this.tokenGenerator.hash(RAW_TOKEN))
                .thenReturn(TOKEN_HASH);
        when(this.tokenRepository.findValid(auth.getId(), TOKEN_HASH, VerificationTokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.verifyEmailUseCase.execute(new VerifyEmailRequest(auth.getId(), RAW_TOKEN, PASSWORD))
        );

        verify(this.tokenRepository).findValid(auth.getId(), TOKEN_HASH, VerificationTokenType.EMAIL_VERIFICATION);
        verifyNoInteractions(this.authenticationRepository);
        verifyNoInteractions(this.passwordHasher);
        verifyNoInteractions(this.eventPublisher);
        verify(this.tokenRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserDoesNotExist() {
        Authentication auth = this.createAuth();
        VerificationToken token = this.createEmailVerificationToken(auth);

        when(this.tokenGenerator.hash(RAW_TOKEN))
                .thenReturn(TOKEN_HASH);
        when(this.tokenRepository.findValid(auth.getId(), TOKEN_HASH, VerificationTokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(token));
        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.verifyEmailUseCase.execute(new VerifyEmailRequest(auth.getId(), RAW_TOKEN, PASSWORD))
        );

        verifyNoInteractions(this.passwordHasher);
        verifyNoInteractions(this.eventPublisher);
        verify(this.authenticationRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(this.tokenRepository, never()).save(token);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserIsDeleted() {
        Authentication auth = this.createAuth();
        auth.deleteUser();
        VerificationToken token = this.createEmailVerificationToken(auth);

        when(this.tokenGenerator.hash(RAW_TOKEN))
                .thenReturn(TOKEN_HASH);
        when(this.tokenRepository.findValid(auth.getId(), TOKEN_HASH, VerificationTokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(token));
        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.verifyEmailUseCase.execute(new VerifyEmailRequest(auth.getId(), RAW_TOKEN, PASSWORD))
        );

        verifyNoInteractions(this.passwordHasher);
        verifyNoInteractions(this.eventPublisher);
        verify(this.authenticationRepository, never()).save(auth);
        verify(this.tokenRepository, never()).save(token);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserIsInactive() {
        Authentication auth = this.createInactiveAuth();
        VerificationToken token = this.createEmailVerificationToken(auth);

        when(this.tokenGenerator.hash(RAW_TOKEN))
                .thenReturn(TOKEN_HASH);
        when(this.tokenRepository.findValid(auth.getId(), TOKEN_HASH, VerificationTokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(token));
        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.verifyEmailUseCase.execute(new VerifyEmailRequest(auth.getId(), RAW_TOKEN, PASSWORD))
        );

        verifyNoInteractions(this.passwordHasher);
        verifyNoInteractions(this.eventPublisher);
        verify(this.authenticationRepository, never()).save(auth);
        verify(this.tokenRepository, never()).save(token);
    }

    @Test
    void execute_shouldIncrementAttemptsAndSaveTokenWhenPasswordIsWrong() {
        Authentication auth = this.createAuth();
        VerificationToken token = this.createEmailVerificationToken(auth);
        int attemptsBefore = token.getAttempts();

        when(this.tokenGenerator.hash(RAW_TOKEN))
                .thenReturn(TOKEN_HASH);
        when(this.tokenRepository.findValid(auth.getId(), TOKEN_HASH, VerificationTokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(token));
        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));
        when(this.passwordHasher.checkPassword("WrongPass1!", auth.getPasswordHash()))
                .thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.verifyEmailUseCase.execute(new VerifyEmailRequest(auth.getId(), RAW_TOKEN, "WrongPass1!"))
        );

        assertFalse(auth.isEmailVerified());
        assertEquals(attemptsBefore + 1, token.getAttempts());
        assertTrue(token.isValid());
        verify(this.tokenRepository).save(token);
        verify(this.authenticationRepository, never()).save(auth);
        verifyNoInteractions(this.eventPublisher);
    }
}
