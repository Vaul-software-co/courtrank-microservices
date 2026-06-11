package com.courtrank.authService.infrastructure.config;

import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.ports.security.TokenHasher;
import com.courtrank.authService.application.ports.security.TokenService;
import com.courtrank.authService.application.useCases.ListSessionsUseCase;
import com.courtrank.authService.application.useCases.LogoutUseCase;
import com.courtrank.authService.application.useCases.RefreshSessionUseCase;
import com.courtrank.authService.application.useCases.RevokeAllSessionsUseCase;
import com.courtrank.authService.application.useCases.RevokeSessionUseCase;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.domain.repository.SessionRepository;
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
