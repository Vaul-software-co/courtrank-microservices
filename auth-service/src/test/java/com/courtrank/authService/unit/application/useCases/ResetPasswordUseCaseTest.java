package com.courtrank.authService.unit.application.useCases;

import com.courtrank.authService.application.dto.ResetPasswordRequest;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.ports.security.PasswordHasher;
import com.courtrank.authService.application.ports.security.TokenHasher;
import com.courtrank.authService.application.ports.security.TokenService;
import com.courtrank.authService.application.useCases.ResetPasswordUseCase;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.entity.VerificationToken;
import com.courtrank.authService.domain.enums.UserRole;
import com.courtrank.authService.domain.enums.VerificationTokenType;
import com.courtrank.authService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.authService.domain.exceptions.WeakPasswordException;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.domain.repository.VerificationTokenRepository;
import com.courtrank.authService.domain.service.PasswordPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ResetPasswordUseCaseTest {
    @Mock
    AuthenticationRepository authenticationRepository;

    @Mock
    PasswordHasher passwordHasher;

    @Mock
    TokenService tokenService;

    @Mock
    TokenHasher tokenHasher;

    @Mock
    VerificationTokenRepository verificationTokenRepository;

    @Mock
    PasswordPolicy passwordPolicy;

    @Mock
    AuditLogger auditLogger;

    @InjectMocks
    ResetPasswordUseCase resetPasswordUseCase;

    private static final String EMAIL = "test@test.com";
    private static final String PASSWORD_HASH = "hash";
    private static final UserRole ROLE = UserRole.MEMBER;
    private static final String RESET_TOKEN = "reset-token";
    private static final UUID RESET_TOKEN_ID = UUID.fromString("0c078bf6-7653-46bf-b02c-5b7049f6f64a");
    private static final String RESET_TOKEN_ID_HASH = "reset-token-id-hash";

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

    private VerificationToken createResetTokenRecord(Authentication auth) {
        return VerificationToken.create(
                auth.getId(),
                RESET_TOKEN_ID_HASH,
                VerificationTokenType.PASSWORD_RESET_CONFIRMATION
        );
    }

    @Test
    void execute_shouldResetPasswordWhenResetTokenIsValid() {
        Authentication auth = this.createAuth();
        VerificationToken resetTokenRecord = this.createResetTokenRecord(auth);
        String newPassword = "NewPass1!";
        String newPasswordHash = "new-hash";

        when(this.tokenService.verifyPasswordReset(RESET_TOKEN))
                .thenReturn(true);
        when(this.tokenService.getTokenId(RESET_TOKEN))
                .thenReturn(auth.getId());
        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));
        when(this.tokenService.getTokenJti(RESET_TOKEN))
                .thenReturn(RESET_TOKEN_ID);
        when(this.tokenHasher.hash(RESET_TOKEN_ID.toString()))
                .thenReturn(RESET_TOKEN_ID_HASH);
        when(this.verificationTokenRepository.findValid(auth.getId(), RESET_TOKEN_ID_HASH, VerificationTokenType.PASSWORD_RESET_CONFIRMATION))
                .thenReturn(Optional.of(resetTokenRecord));
        when(this.passwordHasher.hashPassword(newPassword))
                .thenReturn(newPasswordHash);

        this.resetPasswordUseCase.execute(new ResetPasswordRequest(RESET_TOKEN, newPassword));

        assertEquals(newPasswordHash, auth.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertFalse(resetTokenRecord.isValid());
        verify(this.tokenService).verifyPasswordReset(RESET_TOKEN);
        verify(this.tokenService).getTokenId(RESET_TOKEN);
        verify(this.tokenService).getTokenJti(RESET_TOKEN);
        verify(this.verificationTokenRepository).save(resetTokenRecord);
        verify(this.passwordPolicy).validate(newPassword);
        verify(this.passwordHasher).hashPassword(newPassword);
        verify(this.authenticationRepository).save(auth);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenResetTokenIsInvalid() {
        String newPassword = "NewPass1!";

        when(this.tokenService.verifyPasswordReset(RESET_TOKEN))
                .thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.resetPasswordUseCase.execute(new ResetPasswordRequest(RESET_TOKEN, newPassword))
        );

        verify(this.tokenService).verifyPasswordReset(RESET_TOKEN);
        verify(this.tokenService, never()).getTokenId(RESET_TOKEN);
        verifyNoInteractions(this.authenticationRepository);
        verifyNoInteractions(this.verificationTokenRepository);
        verifyNoInteractions(this.passwordPolicy);
        verifyNoInteractions(this.passwordHasher);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        String newPassword = "NewPass1!";

        when(this.tokenService.verifyPasswordReset(RESET_TOKEN))
                .thenReturn(true);
        when(this.tokenService.getTokenId(RESET_TOKEN))
                .thenReturn(userId);
        when(this.authenticationRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.resetPasswordUseCase.execute(new ResetPasswordRequest(RESET_TOKEN, newPassword))
        );

        verify(this.authenticationRepository).findById(userId);
        verifyNoInteractions(this.verificationTokenRepository);
        verifyNoInteractions(this.passwordPolicy);
        verifyNoInteractions(this.passwordHasher);
        verify(this.authenticationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserIsDeleted() {
        Authentication auth = this.createAuth();
        auth.deleteUser();

        when(this.tokenService.verifyPasswordReset(RESET_TOKEN))
                .thenReturn(true);
        when(this.tokenService.getTokenId(RESET_TOKEN))
                .thenReturn(auth.getId());
        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.resetPasswordUseCase.execute(new ResetPasswordRequest(RESET_TOKEN, "NewPass1!"))
        );

        verifyNoInteractions(this.passwordPolicy);
        verifyNoInteractions(this.passwordHasher);
        verifyNoInteractions(this.verificationTokenRepository);
        verify(this.authenticationRepository, never()).save(auth);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserIsInactive() {
        Authentication auth = this.createInactiveAuth();

        when(this.tokenService.verifyPasswordReset(RESET_TOKEN))
                .thenReturn(true);
        when(this.tokenService.getTokenId(RESET_TOKEN))
                .thenReturn(auth.getId());
        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.resetPasswordUseCase.execute(new ResetPasswordRequest(RESET_TOKEN, "NewPass1!"))
        );

        verifyNoInteractions(this.passwordPolicy);
        verifyNoInteractions(this.passwordHasher);
        verifyNoInteractions(this.verificationTokenRepository);
        verify(this.authenticationRepository, never()).save(auth);
    }

    @Test
    void execute_shouldThrowWeakPasswordWhenNewPasswordIsWeak() {
        Authentication auth = this.createAuth();
        VerificationToken resetTokenRecord = this.createResetTokenRecord(auth);
        String weakPassword = "weak";
        String currentPasswordHash = auth.getPasswordHash();

        when(this.tokenService.verifyPasswordReset(RESET_TOKEN))
                .thenReturn(true);
        when(this.tokenService.getTokenId(RESET_TOKEN))
                .thenReturn(auth.getId());
        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));
        when(this.tokenService.getTokenJti(RESET_TOKEN))
                .thenReturn(RESET_TOKEN_ID);
        when(this.tokenHasher.hash(RESET_TOKEN_ID.toString()))
                .thenReturn(RESET_TOKEN_ID_HASH);
        when(this.verificationTokenRepository.findValid(auth.getId(), RESET_TOKEN_ID_HASH, VerificationTokenType.PASSWORD_RESET_CONFIRMATION))
                .thenReturn(Optional.of(resetTokenRecord));

        doThrow(new WeakPasswordException("weak password"))
                .when(this.passwordPolicy)
                .validate(weakPassword);

        assertThrows(
                WeakPasswordException.class,
                () -> this.resetPasswordUseCase.execute(new ResetPasswordRequest(RESET_TOKEN, weakPassword))
        );

        assertEquals(currentPasswordHash, auth.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertTrue(resetTokenRecord.isValid());
        verify(this.passwordPolicy).validate(weakPassword);
        verify(this.passwordHasher, never()).hashPassword(weakPassword);
        verify(this.verificationTokenRepository, never()).save(resetTokenRecord);
        verify(this.authenticationRepository, never()).save(auth);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenResetTokenRecordWasAlreadyUsed() {
        Authentication auth = this.createAuth();

        when(this.tokenService.verifyPasswordReset(RESET_TOKEN))
                .thenReturn(true);
        when(this.tokenService.getTokenId(RESET_TOKEN))
                .thenReturn(auth.getId());
        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));
        when(this.tokenService.getTokenJti(RESET_TOKEN))
                .thenReturn(RESET_TOKEN_ID);
        when(this.tokenHasher.hash(RESET_TOKEN_ID.toString()))
                .thenReturn(RESET_TOKEN_ID_HASH);
        when(this.verificationTokenRepository.findValid(auth.getId(), RESET_TOKEN_ID_HASH, VerificationTokenType.PASSWORD_RESET_CONFIRMATION))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.resetPasswordUseCase.execute(new ResetPasswordRequest(RESET_TOKEN, "NewPass1!"))
        );

        verifyNoInteractions(this.passwordPolicy);
        verifyNoInteractions(this.passwordHasher);
        verify(this.authenticationRepository, never()).save(auth);
    }
}
