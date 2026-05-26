package com.example.authService.infrastructure.config;

import com.example.authService.application.ports.audit.AuditLogger;
import com.example.authService.application.ports.security.TokenHasher;
import com.example.authService.application.ports.security.TokenService;
import com.example.authService.application.useCases.ListSessionsUseCase;
import com.example.authService.application.useCases.LogoutUseCase;
import com.example.authService.application.useCases.RefreshSessionUseCase;
import com.example.authService.application.useCases.RevokeAllSessionsUseCase;
import com.example.authService.application.useCases.RevokeSessionUseCase;
import com.example.authService.domain.repository.AuthenticationRepository;
import com.example.authService.domain.repository.SessionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SessionUseCaseConfig {

    @Bean
    public RefreshSessionUseCase refreshSessionUseCase(
            TokenService tokenService,
            TokenHasher tokenHasher,
            SessionRepository sessionRepository,
            AuthenticationRepository authenticationRepository,
            AuditLogger auditLogger
    ){
        return new RefreshSessionUseCase(
                tokenService,
                tokenHasher,
                sessionRepository,
                authenticationRepository,
                auditLogger
        );
    }

    @Bean
    public ListSessionsUseCase listSessionsUseCase(
            AuthenticationRepository authenticationRepository,
            SessionRepository sessionRepository,
            AuditLogger auditLogger
    ){
        return new ListSessionsUseCase(
                authenticationRepository,
                sessionRepository,
                auditLogger
        );
    }

    @Bean
    public LogoutUseCase logoutUseCase(
            SessionRepository sessionRepository,
            TokenHasher tokenHasher,
            AuditLogger auditLogger
    ) {
        return new LogoutUseCase(
                sessionRepository,
                tokenHasher,
                auditLogger
        );
    }

    @Bean
    public RevokeSessionUseCase revokeSessionsUseCase(
            AuthenticationRepository authenticationRepository,
            SessionRepository sessionRepository,
            AuditLogger auditLogger
    ){
        return new RevokeSessionUseCase(
                authenticationRepository,
                sessionRepository,
                auditLogger
        );
    }

    @Bean
    public RevokeAllSessionsUseCase revokeAllSessionsUseCase(
            AuthenticationRepository authenticationRepository,
            SessionRepository sessionRepository,
            AuditLogger auditLogger
    ){
        return new RevokeAllSessionsUseCase(
                authenticationRepository,
                sessionRepository,
                auditLogger
        );
    }
}
