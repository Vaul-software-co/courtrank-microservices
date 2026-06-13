package com.courtrank.authService.infrastructure.config;

import com.courtrank.authService.application.useCases.CleanupAuthArtifactsUseCase;
import com.courtrank.authService.domain.repository.SessionRepository;
import com.courtrank.authService.domain.repository.VerificationTokenRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class MaintenanceConfig {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public CleanupAuthArtifactsUseCase cleanupAuthArtifactsUseCase(
            SessionRepository sessionRepository,
            VerificationTokenRepository verificationTokenRepository,
            Clock clock
    ) {
        return new CleanupAuthArtifactsUseCase(
                sessionRepository,
                verificationTokenRepository,
                clock
        );
    }
}
