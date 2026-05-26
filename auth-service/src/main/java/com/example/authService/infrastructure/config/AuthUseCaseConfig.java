package com.example.authService.infrastructure.config;

import com.example.authService.application.ports.AuthEventPublisher;
import com.example.authService.application.ports.audit.AuditLogger;
import com.example.authService.application.ports.authorization.WorkerAccessVerifier;
import com.example.authService.application.ports.security.PasswordHasher;
import com.example.authService.application.ports.security.TokenHasher;
import com.example.authService.application.ports.security.TokenService;
import com.example.authService.application.useCases.DeleteUserUseCase;
import com.example.authService.application.useCases.SignInUseCase;
import com.example.authService.application.useCases.SignUpUseCase;
import com.example.authService.domain.repository.AuthenticationRepository;
import com.example.authService.domain.repository.SessionRepository;
import com.example.authService.domain.service.PasswordPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthUseCaseConfig {

    @Bean
    public SignInUseCase signInUseCase(
            AuthenticationRepository authenticationRepository,
            PasswordHasher passwordHasher,
            SessionRepository sessionRepository,
            TokenService tokenService,
            TokenHasher tokenHasher,
            WorkerAccessVerifier workerAccess,
            AuditLogger auditLogger
    ) {
        return new SignInUseCase(
                authenticationRepository,
                passwordHasher,
                sessionRepository,
                tokenService,
                tokenHasher,
                workerAccess,
                auditLogger
        );
    }

    @Bean
    public SignUpUseCase signUpUseCase(
            AuthenticationRepository authenticationRepository,
            PasswordHasher passwordHasher,
            AuthEventPublisher authEventPublisher,
            PasswordPolicy passwordPolicy,
            AuditLogger auditLogger
    ){
        return new SignUpUseCase(
                authenticationRepository,
                passwordHasher,
                authEventPublisher,
                passwordPolicy,
                auditLogger
        );
    }

    @Bean
    public DeleteUserUseCase deleteUserUseCase(
            AuthenticationRepository authenticationRepository,
            SessionRepository sessionRepository,
            AuditLogger auditLogger
    ){
        return new DeleteUserUseCase(
                authenticationRepository,
                sessionRepository,
                auditLogger
        );
    }
}
