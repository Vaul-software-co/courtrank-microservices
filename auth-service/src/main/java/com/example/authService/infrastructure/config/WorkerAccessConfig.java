package com.example.authService.infrastructure.config;

import com.example.authService.application.ports.authorization.WorkerAccessVerifier;
import com.example.authService.infrastructure.authorization.HttpWorkerAccessVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration
@Profile("!test")
public class WorkerAccessConfig {

    @Bean
    public RestClient workerAccessRestClient(
            RestClient.Builder builder,
            @Value("${app.worker-access.service-url}") String serviceUrl
    ) {
        return builder
                .baseUrl(serviceUrl)
                .build();
    }

    @Bean
    public WorkerAccessVerifier workerAccessVerifier(
            RestClient workerAccessRestClient,
            @Value("${app.worker-access.api-key}") String internalApiKey
    ) {
        return new HttpWorkerAccessVerifier(
                workerAccessRestClient,
                internalApiKey
        );
    }
}
