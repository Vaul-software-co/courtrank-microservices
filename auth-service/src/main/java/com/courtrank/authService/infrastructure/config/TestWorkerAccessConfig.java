package com.courtrank.authService.infrastructure.config;

import com.courtrank.authService.application.ports.authorization.WorkerAccessVerifier;
import com.courtrank.authService.infrastructure.authorization.DenyWorkerAccessVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestWorkerAccessConfig {

    @Bean
    public WorkerAccessVerifier workerAccessVerifier() {
        return new DenyWorkerAccessVerifier();
    }
}
