package com.courtrank.socialService.infrastructure.config;

import com.courtrank.socialService.application.ports.SocialEventPublisher;
import com.courtrank.socialService.application.ports.audit.SocialAuditLogger;
import com.courtrank.socialService.infrastructure.audit.KafkaSocialAuditLogger;
import com.courtrank.socialService.infrastructure.events.KafkaSocialEventPublisher;
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
    public SocialAuditLogger socialAuditLogger(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.audit-events}") String topic
    ) {
        return new KafkaSocialAuditLogger(
                kafkaTemplate,
                objectMapper,
                topic
        );
    }

    @Bean
    public SocialEventPublisher socialEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.social-events}") String topic
    ) {
        return new KafkaSocialEventPublisher(
                kafkaTemplate,
                objectMapper,
                topic
        );
    }
}
