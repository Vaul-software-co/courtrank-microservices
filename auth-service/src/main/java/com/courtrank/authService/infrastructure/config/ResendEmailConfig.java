package com.courtrank.authService.infrastructure.config;

import com.courtrank.authService.application.ports.email.EmailSender;
import com.courtrank.authService.infrastructure.email.ResendEmailSender;
import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!local & !test")
public class ResendEmailConfig {

    @Bean
    public Resend resend(@Value("${app.resend.api-key}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("RESEND_API_KEY must be defined");
        }

        return new Resend(apiKey);
    }

    @Bean
    public EmailSender emailSender(
            Resend resend,
            @Value("${app.resend.from}") String from
    ) {
        return new ResendEmailSender(resend, from);
    }
}
