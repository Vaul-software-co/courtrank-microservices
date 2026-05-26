package com.example.authService.infrastructure.config;

import com.example.authService.application.ports.audit.AuditLogger;
import com.example.authService.application.ports.email.EmailSender;
import com.example.authService.application.ports.security.PasswordHasher;
import com.example.authService.application.ports.security.TokenHasher;
import com.example.authService.application.ports.security.TokenService;
import com.example.authService.application.ports.security.VerificationTokenGenerator;
import com.example.authService.application.useCases.ChangePasswordUseCase;
import com.example.authService.application.useCases.RequestPasswordResetUseCase;
import com.example.authService.application.useCases.ResetPasswordUseCase;
import com.example.authService.application.useCases.VerifyPasswordOtpUseCase;
import com.example.authService.domain.repository.AuthenticationRepository;
import com.example.authService.domain.repository.VerificationTokenRepository;
import com.example.authService.domain.service.PasswordPolicy;
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
