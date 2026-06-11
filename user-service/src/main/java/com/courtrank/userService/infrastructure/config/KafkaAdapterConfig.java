package com.courtrank.userService.infrastructure.config;

import com.courtrank.userService.application.ports.UserEventPublisher;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.infrastructure.audit.KafkaUserAuditLogger;
import com.courtrank.userService.infrastructure.events.KafkaUserEventPublisher;
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
    public UserAuditLogger userAuditLogger(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.audit-events}") String topic
    ) {
        return new KafkaUserAuditLogger(
                kafkaTemplate,
                objectMapper,
                topic
        );
    }

    @Bean
    public UserEventPublisher userEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.user-events}") String topic
    ) {
        return new KafkaUserEventPublisher(
                kafkaTemplate,
                objectMapper,
                topic
        );
    }
}
