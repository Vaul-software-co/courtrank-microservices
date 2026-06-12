package com.courtrank.socialService.infrastructure.config;

import com.courtrank.socialService.application.ports.SocialUserProfileProvider;
import com.courtrank.socialService.infrastructure.user.HttpSocialUserProfileProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class UserServiceConfig {
    @Bean
    public RestClient userServiceRestClient(RestClient.Builder builder, @Value("${app.user-service.url}") String serviceUrl) {
        return builder.baseUrl(serviceUrl).build();
    }

    @Bean
    public SocialUserProfileProvider socialUserProfileProvider(
            RestClient userServiceRestClient,
            @Value("${app.user-service.api-key:${app.internal-api-key}}") String internalApiKey
    ) {
        return new HttpSocialUserProfileProvider(userServiceRestClient, internalApiKey);
    }
}
