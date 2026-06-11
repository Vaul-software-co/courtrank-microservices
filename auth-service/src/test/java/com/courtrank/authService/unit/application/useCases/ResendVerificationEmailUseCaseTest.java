package com.courtrank.authService.unit.application.useCases;

import com.courtrank.authService.application.dto.ResendVerificationEmailRequest;
import com.courtrank.authService.application.dto.VerificationEmailRequest;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.useCases.ResendVerificationEmailUseCase;
import com.courtrank.authService.application.useCases.SendVerificationEmailUseCase;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.enums.UserRole;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ResendVerificationEmailUseCaseTest {
    @Mock
    AuthenticationRepository authenticationRepository;

    @Mock
    SendVerificationEmailUseCase sendEmail;

    @Mock
    AuditLogger auditLogger;

    @InjectMocks
    ResendVerificationEmailUseCase resendVerificationEmailUseCase;

    private static final String EMAIL = "test@test.com";
    private static final String PASSWORD_HASH = "hash";
    private static final String LANG = "es";

    private Authentication createAuth() {
        return Authentication.create(
                EMAIL,
                PASSWORD_HASH,
                UserRole.MEMBER
        );
    }

    @Test
    void execute_shouldSendVerificationEmailWhenUserExistsAndIsNotVerified() {
        Authentication auth = this.createAuth();

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(auth));

        this.resendVerificationEmailUseCase.execute(new ResendVerificationEmailRequest(EMAIL, LANG));

        ArgumentCaptor<VerificationEmailRequest> requestCaptor = ArgumentCaptor.forClass(VerificationEmailRequest.class);
        verify(this.sendEmail).execute(requestCaptor.capture());

        VerificationEmailRequest sentRequest = requestCaptor.getValue();
        assertEquals(auth.getId(), sentRequest.id());
        assertEquals(auth.getEmail(), sentRequest.email());
        assertEquals(LANG, sentRequest.lang());
    }

    @Test
    void execute_shouldDoNothingWhenUserDoesNotExist() {
        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        this.resendVerificationEmailUseCase.execute(new ResendVerificationEmailRequest(EMAIL, LANG));

        verify(this.sendEmail, never()).execute(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void execute_shouldDoNothingWhenUserIsAlreadyVerified() {
        Authentication auth = this.createAuth();
        auth.verifyEmail();

        when(this.authenticationRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(auth));

        this.resendVerificationEmailUseCase.execute(new ResendVerificationEmailRequest(EMAIL, LANG));

        verify(this.sendEmail, never()).execute(org.mockito.ArgumentMatchers.any());
    }
}
