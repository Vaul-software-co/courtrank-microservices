package com.courtrank.authService.infrastructure.config;

import com.courtrank.authService.application.ports.AuthEventPublisher;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.ports.email.EmailSender;
import com.courtrank.authService.application.ports.security.PasswordHasher;
import com.courtrank.authService.application.ports.security.VerificationTokenGenerator;
import com.courtrank.authService.application.useCases.ResendVerificationEmailUseCase;
import com.courtrank.authService.application.useCases.SendVerificationEmailUseCase;
import com.courtrank.authService.application.useCases.VerifyEmailUseCase;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.domain.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailUseCaseConfig {

    @Bean
    public SendVerificationEmailUseCase sendVerificationEmailUseCase(
            VerificationTokenGenerator verificationTokenGenerator,
            VerificationTokenRepository verificationTokenRepository,
            @Value("${app.frontend-url}") String frontendUrl,
            EmailSender emailSender,
            AuditLogger auditLogger
    ){
        return new SendVerificationEmailUseCase(
                verificationTokenGenerator,
                verificationTokenRepository,
                frontendUrl,
                emailSender,
                auditLogger
        );
    }

    @Bean
    public ResendVerificationEmailUseCase resendVerificationEmailUseCase(
            AuthenticationRepository authenticationRepository,
            SendVerificationEmailUseCase sendVerificationEmailUseCase,
            AuditLogger auditLogger
    ) {
        return new ResendVerificationEmailUseCase(
                authenticationRepository,
                sendVerificationEmailUseCase,
                auditLogger
        );
    }

    @Bean
    public VerifyEmailUseCase verifyEmailUseCase(
            VerificationTokenRepository verificationTokenRepository,
            VerificationTokenGenerator verificationTokenGenerator,
            AuthenticationRepository authenticationRepository,
            PasswordHasher passwordHasher,
            AuditLogger auditLogger,
            AuthEventPublisher authEventPublisher
    ) {
        return new VerifyEmailUseCase(
                verificationTokenRepository,
                verificationTokenGenerator,
                authenticationRepository,
                passwordHasher,
                auditLogger,
                authEventPublisher
        );
    }
}
