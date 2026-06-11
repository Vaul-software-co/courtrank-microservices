package com.courtrank.authService.infrastructure.config;

import com.courtrank.authService.application.ports.AuthEventPublisher;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.infrastructure.audit.KafkaAuditLogger;
import com.courtrank.authService.infrastructure.events.KafkaAuthEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@Profile({"kafka", "production"})
public class KafkaAdapterConfig {

    @Bean
    public AuthEventPublisher authEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.auth-events}") String topic
    ) {
        return new KafkaAuthEventPublisher(
                kafkaTemplate,
                objectMapper,
                topic
        );
    }

    @Bean
    public AuditLogger auditLogger(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.audit-events}") String topic
    ) {
        return new KafkaAuditLogger(
                kafkaTemplate,
                objectMapper,
                topic
        );
    }
}
