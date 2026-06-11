package com.courtrank.authService.infrastructure.config;

import com.courtrank.authService.application.ports.AuthEventPublisher;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.ports.authorization.WorkerAccessVerifier;
import com.courtrank.authService.application.ports.security.PasswordHasher;
import com.courtrank.authService.application.ports.security.TokenHasher;
import com.courtrank.authService.application.ports.security.TokenService;
import com.courtrank.authService.application.ports.user.UsernameAvailabilityVerifier;
import com.courtrank.authService.application.services.SessionIssuer;
import com.courtrank.authService.application.useCases.DeleteUserUseCase;
import com.courtrank.authService.application.useCases.SignInUseCase;
import com.courtrank.authService.application.useCases.SignUpUseCase;
import com.courtrank.authService.application.useCases.UpdateDataConsentUseCase;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.domain.repository.SessionRepository;
import com.courtrank.authService.domain.service.PasswordPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthUseCaseConfig {

    @Bean
    public SignInUseCase signInUseCase(
            AuthenticationRepository authenticationRepository,
            PasswordHasher passwordHasher,
            WorkerAccessVerifier workerAccess,
            AuditLogger auditLogger,
            SessionIssuer sessionIssuer
    ) {
        return new SignInUseCase(
                authenticationRepository,
                passwordHasher,
                workerAccess,
                auditLogger,
                sessionIssuer
        );
    }

    @Bean
    public SessionIssuer sessionIssuer(
            SessionRepository sessionRepository,
            TokenService tokenService,
            TokenHasher tokenHasher
    ) {
        return new SessionIssuer(sessionRepository, tokenService, tokenHasher);
    }

    @Bean
    public SignUpUseCase signUpUseCase(
            AuthenticationRepository authenticationRepository,
            PasswordHasher passwordHasher,
            AuthEventPublisher authEventPublisher,
            UsernameAvailabilityVerifier usernameAvailabilityVerifier,
            PasswordPolicy passwordPolicy,
            AuditLogger auditLogger,
            SessionIssuer sessionIssuer
    ){
        return new SignUpUseCase(
                authenticationRepository,
                passwordHasher,
                authEventPublisher,
                usernameAvailabilityVerifier,
                passwordPolicy,
                auditLogger,
                sessionIssuer
        );
    }

    @Bean
    public DeleteUserUseCase deleteUserUseCase(
            AuthenticationRepository authenticationRepository,
            SessionRepository sessionRepository,
            AuthEventPublisher authEventPublisher,
            AuditLogger auditLogger
    ){
        return new DeleteUserUseCase(
                authenticationRepository,
                sessionRepository,
                authEventPublisher,
                auditLogger
        );
    }

    @Bean
    public UpdateDataConsentUseCase updateDataConsentUseCase(
            AuthenticationRepository authenticationRepository,
            AuditLogger auditLogger
    ) {
        return new UpdateDataConsentUseCase(authenticationRepository, auditLogger);
    }
}
