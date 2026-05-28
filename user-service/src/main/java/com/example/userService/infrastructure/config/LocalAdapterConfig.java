package com.example.userService.infrastructure.config;

import com.example.userService.application.ports.UserEventPublisher;
import com.example.userService.application.ports.audit.UserAuditLogger;
import com.example.userService.infrastructure.audit.ConsoleUserAuditLogger;
import com.example.userService.infrastructure.events.ConsoleUserEventPublisher;
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
