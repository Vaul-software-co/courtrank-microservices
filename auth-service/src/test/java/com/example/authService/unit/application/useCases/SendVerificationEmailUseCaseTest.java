package com.example.authService.unit.application.useCases;

import com.example.authService.application.dto.VerificationEmailRequest;
import com.example.authService.application.ports.audit.AuditLogger;
import com.example.authService.application.ports.email.EmailSender;
import com.example.authService.application.ports.security.VerificationTokenGenerator;
import com.example.authService.application.useCases.SendVerificationEmailUseCase;
import com.example.authService.domain.entity.VerificationToken;
import com.example.authService.domain.enums.VerificationTokenType;
import com.example.authService.domain.repository.VerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SendVerificationEmailUseCaseTest {
    @Mock
    VerificationTokenGenerator tokenGenerator;

    @Mock
    VerificationTokenRepository verificationTokenRepository;

    @Mock
    EmailSender emailSender;

    @Mock
    AuditLogger auditLogger;

    private static final String FRONTEND_URL = "https://webapp-test.getcourtrank.com";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "test@test.com";
    private static final String LANG = "es";
    private static final String RAW_TOKEN = "raw-token";
    private static final String TOKEN_HASH = "token-hash";

    private SendVerificationEmailUseCase createUseCase() {
        return new SendVerificationEmailUseCase(
                this.tokenGenerator,
                this.verificationTokenRepository,
                FRONTEND_URL,
                this.emailSender,
                this.auditLogger
        );
    }

    @Test
    void execute_shouldInvalidatePreviousSaveTokenAndSendVerificationEmail() {
        when(this.tokenGenerator.generateUrlToken())
                .thenReturn(RAW_TOKEN);
        when(this.tokenGenerator.hash(RAW_TOKEN))
                .thenReturn(TOKEN_HASH);

        this.createUseCase().execute(new VerificationEmailRequest(USER_ID, EMAIL, LANG));

        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
        String expectedLink = FRONTEND_URL + "/verify-email?token=" + RAW_TOKEN + "&userId=" + USER_ID;

        verify(this.verificationTokenRepository).invalidatePrevious(USER_ID, VerificationTokenType.EMAIL_VERIFICATION);
        verify(this.verificationTokenRepository).save(tokenCaptor.capture());
        verify(this.emailSender).sendEmailVerification(EMAIL, expectedLink, LANG);

        VerificationToken savedToken = tokenCaptor.getValue();
        assertEquals(USER_ID, savedToken.getUserId());
        assertEquals(TOKEN_HASH, savedToken.getTokenHash());
        assertEquals(VerificationTokenType.EMAIL_VERIFICATION, savedToken.getType());
        assertTrue(savedToken.isValid());
    }
}
