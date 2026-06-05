package com.example.authService.infrastructure.config;

import com.example.authService.application.ports.user.UsernameAvailabilityVerifier;
import com.example.authService.infrastructure.user.HttpUsernameAvailabilityVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration
@Profile("!test & (!local | kafka | production)")
public class UserServiceConfig {

    @Bean
    public RestClient userServiceRestClient(
            RestClient.Builder builder,
            @Value("${app.user-service.url}") String serviceUrl
    ) {
        return builder.baseUrl(serviceUrl).build();
    }

    @Bean
    @ConditionalOnMissingBean(UsernameAvailabilityVerifier.class)
    public UsernameAvailabilityVerifier usernameAvailabilityVerifier(
            RestClient userServiceRestClient,
            @Value("${app.user-service.api-key}") String internalApiKey
    ) {
        return new HttpUsernameAvailabilityVerifier(userServiceRestClient, internalApiKey);
    }
}
