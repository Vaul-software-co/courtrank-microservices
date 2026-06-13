package com.courtrank.authService.infrastructure.maintenance;

import com.courtrank.authService.application.dto.AuthCleanupResult;
import com.courtrank.authService.application.useCases.CleanupAuthArtifactsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "app.maintenance.cleanup.enabled", havingValue = "true")
public class AuthCleanupScheduler {
    private static final Logger logger = LoggerFactory.getLogger(AuthCleanupScheduler.class);

    private final CleanupAuthArtifactsUseCase cleanupAuthArtifactsUseCase;
    private final Duration sessionRetention;
    private final Duration verificationTokenRetention;

    public AuthCleanupScheduler(
            CleanupAuthArtifactsUseCase cleanupAuthArtifactsUseCase,
            @Value("${app.maintenance.cleanup.session-retention}") Duration sessionRetention,
            @Value("${app.maintenance.cleanup.verification-token-retention}") Duration verificationTokenRetention
    ) {
        this.cleanupAuthArtifactsUseCase = cleanupAuthArtifactsUseCase;
        this.sessionRetention = sessionRetention;
        this.verificationTokenRetention = verificationTokenRetention;
    }

    @Scheduled(cron = "${app.maintenance.cleanup.cron}", zone = "${app.maintenance.cleanup.zone}")
    public void run() {
        AuthCleanupResult result = this.cleanupAuthArtifactsUseCase.execute(
                this.sessionRetention,
                this.verificationTokenRetention
        );

        logger.info(
                "auth cleanup completed deletedSessions={} deletedVerificationTokens={} sessionCutoff={} verificationTokenCutoff={}",
                result.deletedSessions(),
                result.deletedVerificationTokens(),
                result.sessionCutoff(),
                result.verificationTokenCutoff()
        );
    }
}
