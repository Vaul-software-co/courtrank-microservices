package com.courtrank.authService.infrastructure.config;

import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.ports.email.EmailSender;
import com.courtrank.authService.application.ports.security.PasswordHasher;
import com.courtrank.authService.application.ports.security.TokenHasher;
import com.courtrank.authService.application.ports.security.TokenService;
import com.courtrank.authService.application.ports.security.VerificationTokenGenerator;
import com.courtrank.authService.application.useCases.ChangePasswordUseCase;
import com.courtrank.authService.application.useCases.RequestPasswordResetUseCase;
import com.courtrank.authService.application.useCases.ResetPasswordUseCase;
import com.courtrank.authService.application.useCases.VerifyPasswordOtpUseCase;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.domain.repository.VerificationTokenRepository;
import com.courtrank.authService.domain.service.PasswordPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PasswordUseCaseConfig {
    @Bean
    public ChangePasswordUseCase changePasswordUseCase(
            AuthenticationRepository authenticationRepository,
            PasswordHasher passwordHasher,
            PasswordPolicy passwordPolicy,
            AuditLogger auditLogger
    ){
        return new ChangePasswordUseCase(
                authenticationRepository,
                passwordHasher,
                passwordPolicy,
                auditLogger
        );
    }

    @Bean
    public RequestPasswordResetUseCase requestPasswordResetUseCase(
            VerificationTokenRepository verificationTokenRepository,
            AuthenticationRepository authenticationRepository,
            VerificationTokenGenerator verificationTokenGenerator,
            EmailSender emailSender,
            AuditLogger auditLogger
    ){
        return new RequestPasswordResetUseCase(
                verificationTokenRepository,
                authenticationRepository,
                verificationTokenGenerator,
                emailSender,
                auditLogger
        );
    }

    @Bean
    public VerifyPasswordOtpUseCase verifyPasswordOtpUseCase(
            VerificationTokenRepository verificationTokenRepository,
            VerificationTokenGenerator verificationTokenGenerator,
            AuthenticationRepository authenticationRepository,
            TokenService tokenService,
            TokenHasher tokenHasher,
            AuditLogger auditLogger
    ){
        return new VerifyPasswordOtpUseCase(
                verificationTokenRepository,
                verificationTokenGenerator,
                authenticationRepository,
                tokenService,
                tokenHasher,
                auditLogger
        );
    }

    @Bean
    public ResetPasswordUseCase resetPasswordUseCase(
            AuthenticationRepository authenticationRepository,
            PasswordHasher passwordHasher,
            TokenService tokenService,
            TokenHasher tokenHasher,
            VerificationTokenRepository verificationTokenRepository,
            PasswordPolicy passwordPolicy,
            AuditLogger auditLogger
    ){
        return new ResetPasswordUseCase(
                authenticationRepository,
                passwordHasher,
                tokenService,
                tokenHasher,
                verificationTokenRepository,
                passwordPolicy,
                auditLogger
        );
    }
}
