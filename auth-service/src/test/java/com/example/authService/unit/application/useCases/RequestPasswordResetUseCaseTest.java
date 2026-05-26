package com.example.authService.unit.application.useCases;

import com.example.authService.application.dto.RequestPasswordResetRequest;
import com.example.authService.application.ports.audit.AuditLogger;
import com.example.authService.application.ports.email.EmailSender;
import com.example.authService.application.ports.security.VerificationTokenGenerator;
import com.example.authService.application.useCases.RequestPasswordResetUseCase;
import com.example.authService.domain.entity.Authentication;
import com.example.authService.domain.entity.VerificationToken;
import com.example.authService.domain.enums.UserRole;
import com.example.authService.domain.enums.VerificationTokenType;
import com.example.authService.domain.repository.AuthenticationRepository;
import com.example.authService.domain.repository.VerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RequestPasswordResetUseCaseTest {
    @Mock
    VerificationTokenRepository tokenRepository;

    @Mock
    AuthenticationRepository authenticationRepository;

    @Mock
    VerificationTokenGenerator tokenGenerator;

    @Mock
    EmailSender emailSender;

    @Mock
    AuditLogger auditLogger;

    @InjectMocks
    RequestPasswordResetUseCase requestPasswordResetUseCase;

    private static final String EMAIL = "test@test.com";
    private static final String PASSWORD_HASH = "hash";
    private static final UserRole ROLE = UserRole.MEMBER;
    private static final String LANG = "es";

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

    @Test
    void execute_shouldGenerateOtpInvalidatePreviousSaveTokenAndSendEmailWhenUserIsActive() {
        Authentication auth = this.createAuth();
        String otp = "123456";
        String otpHash = "otp-hash";

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(auth));
        when(this.tokenGenerator.generateOtp())
                .thenReturn(otp);
        when(this.tokenGenerator.hash(otp))
                .thenReturn(otpHash);

        this.requestPasswordResetUseCase.execute(new RequestPasswordResetRequest(EMAIL, LANG));

        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(this.tokenRepository).invalidatePrevious(auth.getId(), VerificationTokenType.PASSWORD_RESET);
        verify(this.tokenRepository).save(tokenCaptor.capture());
        verify(this.emailSender).sendPasswordOtp(auth.getEmail(), otp, LANG);

        VerificationToken savedToken = tokenCaptor.getValue();
        assertEquals(auth.getId(), savedToken.getUserId());
        assertEquals(otpHash, savedToken.getTokenHash());
        assertEquals(VerificationTokenType.PASSWORD_RESET, savedToken.getType());
        assertTrue(savedToken.isValid());
    }

    @Test
    void execute_shouldDoNothingWhenEmailDoesNotExist() {
        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        this.requestPasswordResetUseCase.execute(new RequestPasswordResetRequest(EMAIL, LANG));

        verify(this.authenticationRepository).findByEmail(EMAIL);
        verifyNoInteractions(this.tokenGenerator);
        verifyNoInteractions(this.tokenRepository);
        verifyNoInteractions(this.emailSender);
    }

    @Test
    void execute_shouldDoNothingWhenUserIsDeleted() {
        Authentication auth = this.createAuth();
        auth.deleteUser();

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(auth));

        this.requestPasswordResetUseCase.execute(new RequestPasswordResetRequest(EMAIL, LANG));

        verify(this.authenticationRepository).findByEmail(EMAIL);
        verifyNoInteractions(this.tokenGenerator);
        verify(this.tokenRepository, never()).invalidatePrevious(auth.getId(), VerificationTokenType.PASSWORD_RESET);
        verifyNoInteractions(this.emailSender);
    }

    @Test
    void execute_shouldDoNothingWhenUserIsInactive() {
        Authentication auth = this.createInactiveAuth();

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(auth));

        this.requestPasswordResetUseCase.execute(new RequestPasswordResetRequest(EMAIL, LANG));

        verify(this.authenticationRepository).findByEmail(EMAIL);
        verifyNoInteractions(this.tokenGenerator);
        verify(this.tokenRepository, never()).invalidatePrevious(auth.getId(), VerificationTokenType.PASSWORD_RESET);
        verifyNoInteractions(this.emailSender);
    }
}
