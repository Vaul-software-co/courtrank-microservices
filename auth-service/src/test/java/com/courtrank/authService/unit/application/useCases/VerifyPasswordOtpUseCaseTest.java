package com.courtrank.authService.unit.application.useCases;

import com.courtrank.authService.application.dto.VerifyPasswordOtpRequest;
import com.courtrank.authService.application.dto.VerifyPasswordOtpResponse;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.ports.security.TokenHasher;
import com.courtrank.authService.application.ports.security.TokenService;
import com.courtrank.authService.application.ports.security.VerificationTokenGenerator;
import com.courtrank.authService.application.useCases.VerifyPasswordOtpUseCase;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.entity.VerificationToken;
import com.courtrank.authService.domain.enums.UserRole;
import com.courtrank.authService.domain.enums.VerificationTokenType;
import com.courtrank.authService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.domain.repository.VerificationTokenRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VerifyPasswordOtpUseCaseTest {
    @Mock
    VerificationTokenRepository tokenRepository;

    @Mock
    VerificationTokenGenerator tokenGenerator;

    @Mock
    AuthenticationRepository authenticationRepository;

    @Mock
    TokenService tokenService;

    @Mock
    TokenHasher tokenHasher;

    @Mock
    AuditLogger auditLogger;

    @InjectMocks
    VerifyPasswordOtpUseCase verifyPasswordOtpUseCase;

    private static final String EMAIL = "test@test.com";
    private static final String PASSWORD_HASH = "hash";
    private static final UserRole ROLE = UserRole.MEMBER;
    private static final String OTP = "123456";
    private static final String OTP_HASH = "otp-hash";
    private static final String RESET_TOKEN_ID_HASH = "reset-token-id-hash";
    private static final String RESET_TOKEN = "reset-token";

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

    private VerificationToken createPasswordResetToken() {
        return VerificationToken.create(
                java.util.UUID.randomUUID(),
                OTP_HASH,
                VerificationTokenType.PASSWORD_RESET
        );
    }

    private VerificationToken createPasswordResetToken(Authentication auth) {
        return VerificationToken.create(
                auth.getId(),
                OTP_HASH,
                VerificationTokenType.PASSWORD_RESET
        );
    }

    @Test
    void execute_shouldReturnResetTokenWhenOtpIsCorrect() {
        Authentication auth = this.createAuth();
        VerificationToken token = this.createPasswordResetToken(auth);

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(auth));
        when(this.tokenRepository.findValid(auth.getId(), VerificationTokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(token));
        when(this.tokenGenerator.hash(OTP))
                .thenReturn(OTP_HASH);
        when(this.tokenHasher.hash(anyString()))
                .thenReturn(RESET_TOKEN_ID_HASH);
        when(this.tokenService.generatePasswordResetToken(eq(auth.getId()), any(UUID.class)))
                .thenReturn(RESET_TOKEN);

        VerifyPasswordOtpResponse response = this.verifyPasswordOtpUseCase.execute(
                new VerifyPasswordOtpRequest(EMAIL, OTP)
        );

        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);

        assertEquals(RESET_TOKEN, response.resetToken());
        assertFalse(token.isValid());
        verify(this.tokenRepository, times(2)).save(tokenCaptor.capture());
        assertEquals(token.getId(), tokenCaptor.getAllValues().get(0).getId());
        assertEquals(VerificationTokenType.PASSWORD_RESET_CONFIRMATION, tokenCaptor.getAllValues().get(1).getType());
        assertEquals(RESET_TOKEN_ID_HASH, tokenCaptor.getAllValues().get(1).getTokenHash());
        verify(this.tokenService).generatePasswordResetToken(eq(auth.getId()), any(UUID.class));
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserDoesNotExist() {
        Authentication auth = this.createAuth();

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.verifyPasswordOtpUseCase.execute(new VerifyPasswordOtpRequest(EMAIL, OTP))
        );

        verify(this.authenticationRepository).findByEmail(EMAIL);
        verifyNoInteractions(this.tokenRepository);
        verifyNoInteractions(this.tokenGenerator);
        verifyNoInteractions(this.tokenHasher);
        verifyNoInteractions(this.tokenService);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserIsDeleted() {
        Authentication auth = this.createAuth();
        auth.deleteUser();

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(auth));

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.verifyPasswordOtpUseCase.execute(new VerifyPasswordOtpRequest(EMAIL, OTP))
        );

        verifyNoInteractions(this.tokenRepository);
        verifyNoInteractions(this.tokenGenerator);
        verifyNoInteractions(this.tokenHasher);
        verifyNoInteractions(this.tokenService);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserIsInactive() {
        Authentication auth = this.createInactiveAuth();

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(auth));

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.verifyPasswordOtpUseCase.execute(new VerifyPasswordOtpRequest(EMAIL, OTP))
        );

        verifyNoInteractions(this.tokenRepository);
        verifyNoInteractions(this.tokenGenerator);
        verifyNoInteractions(this.tokenHasher);
        verifyNoInteractions(this.tokenService);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenValidPasswordResetTokenDoesNotExist() {
        Authentication auth = this.createAuth();

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(auth));
        when(this.tokenRepository.findValid(auth.getId(), VerificationTokenType.PASSWORD_RESET))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.verifyPasswordOtpUseCase.execute(new VerifyPasswordOtpRequest(EMAIL, OTP))
        );

        verify(this.tokenRepository).findValid(auth.getId(), VerificationTokenType.PASSWORD_RESET);
        verifyNoInteractions(this.tokenGenerator);
        verifyNoInteractions(this.tokenHasher);
        verifyNoInteractions(this.tokenService);
        verify(this.tokenRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenTokenIsInvalid() {
        Authentication auth = this.createAuth();
        VerificationToken token = VerificationToken.restore(
                java.util.UUID.randomUUID(),
                auth.getId(),
                OTP_HASH,
                VerificationTokenType.PASSWORD_RESET,
                Instant.now().minusSeconds(1),
                null,
                0,
                Instant.now().minusSeconds(60)
        );

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(auth));
        when(this.tokenRepository.findValid(auth.getId(), VerificationTokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(token));

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.verifyPasswordOtpUseCase.execute(new VerifyPasswordOtpRequest(EMAIL, OTP))
        );

        verifyNoInteractions(this.tokenGenerator);
        verifyNoInteractions(this.tokenHasher);
        verifyNoInteractions(this.tokenService);
        verify(this.tokenRepository, never()).save(token);
    }

    @Test
    void execute_shouldIncrementAttemptsAndSaveTokenWhenOtpIsWrong() {
        Authentication auth = this.createAuth();
        VerificationToken token = this.createPasswordResetToken(auth);
        int attemptsBefore = token.getAttempts();

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(auth));
        when(this.tokenRepository.findValid(auth.getId(), VerificationTokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(token));
        when(this.tokenGenerator.hash("wrong-otp"))
                .thenReturn("wrong-hash");

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.verifyPasswordOtpUseCase.execute(new VerifyPasswordOtpRequest(EMAIL, "wrong-otp"))
        );

        assertEquals(attemptsBefore + 1, token.getAttempts());
        assertTrue(token.isValid());
        verify(this.tokenRepository).save(token);
        verifyNoInteractions(this.tokenHasher);
        verifyNoInteractions(this.tokenService);
    }
}
