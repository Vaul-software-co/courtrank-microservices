package com.courtrank.socialService.infrastructure.config;

import com.courtrank.socialService.application.ports.SocialEventPublisher;
import com.courtrank.socialService.application.ports.audit.SocialAuditLogger;
import com.courtrank.socialService.infrastructure.audit.ConsoleSocialAuditLogger;
import com.courtrank.socialService.infrastructure.events.ConsoleSocialEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!kafka & !production")
public class LocalAdapterConfig {
    @Bean
    @ConditionalOnMissingBean(SocialAuditLogger.class)
    public SocialAuditLogger socialAuditLogger() {
        return new ConsoleSocialAuditLogger();
    }

    @Bean
    @ConditionalOnMissingBean(SocialEventPublisher.class)
    public SocialEventPublisher socialEventPublisher() {
        return new ConsoleSocialEventPublisher();
    }
}
