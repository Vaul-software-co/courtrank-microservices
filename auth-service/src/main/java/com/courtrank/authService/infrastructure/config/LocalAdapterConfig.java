package com.courtrank.authService.infrastructure.config;

import com.courtrank.authService.application.ports.AuthEventPublisher;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.ports.email.EmailSender;
import com.courtrank.authService.application.ports.user.UsernameAvailabilityVerifier;
import com.courtrank.authService.infrastructure.audit.ConsoleAuditLogger;
import com.courtrank.authService.infrastructure.email.ConsoleEmailSender;
import com.courtrank.authService.infrastructure.events.ConsoleAuthEventPublisher;
import com.courtrank.authService.infrastructure.user.NoOpUsernameAvailabilityVerifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"local", "test"})
public class LocalAdapterConfig {
    @Bean
    @ConditionalOnMissingBean(AuditLogger.class)
    public AuditLogger auditLogger() {
        return new ConsoleAuditLogger();
    }

    @Bean
    @ConditionalOnMissingBean(AuthEventPublisher.class)
    public AuthEventPublisher authEventPublisher() {
        return new ConsoleAuthEventPublisher();
    }

    @Bean
    @ConditionalOnMissingBean(EmailSender.class)
    public EmailSender emailSender() {
        return new ConsoleEmailSender();
    }

    @Bean
    @ConditionalOnMissingBean(UsernameAvailabilityVerifier.class)
    public UsernameAvailabilityVerifier usernameAvailabilityVerifier() {
        return new NoOpUsernameAvailabilityVerifier();
    }
}
