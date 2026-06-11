package com.courtrank.userService.infrastructure.config;

import com.courtrank.userService.application.ports.UserEventPublisher;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.infrastructure.audit.ConsoleUserAuditLogger;
import com.courtrank.userService.infrastructure.events.ConsoleUserEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local & !kafka & !production")
public class LocalAdapterConfig {
    @Bean
    public UserAuditLogger userAuditLogger() {
        return new ConsoleUserAuditLogger();
    }

    @Bean
    public UserEventPublisher userEventPublisher() {
        return new ConsoleUserEventPublisher();
    }
}
