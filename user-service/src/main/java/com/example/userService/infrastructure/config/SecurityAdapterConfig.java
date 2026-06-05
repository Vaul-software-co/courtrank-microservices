package com.example.userService.infrastructure.config;

import com.example.userService.application.ports.security.AuthSessionVerifier;
import com.example.userService.application.ports.security.TokenService;
import com.example.userService.infrastructure.security.HttpAuthSessionVerifier;
import com.example.userService.infrastructure.security.RsaJwtTokenService;
import com.example.userService.infrastructure.security.RsaPemKeyLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestClient;

import java.security.PublicKey;

@Configuration
public class SecurityAdapterConfig {
    @Bean
    public RsaPemKeyLoader rsaPemKeyLoader() {
        return new RsaPemKeyLoader();
    }

    @Bean
    public PublicKey jwtPublicKey(
            RsaPemKeyLoader keyLoader,
            @Value("${app.jwt.public-key}") Resource publicKeyResource
    ) {
        return keyLoader.loadPublicKey(publicKeyResource);
    }

    @Bean
    public TokenService tokenService(
            PublicKey jwtPublicKey,
            ObjectMapper objectMapper
    ) {
        return new RsaJwtTokenService(jwtPublicKey, objectMapper);
    }

    @Bean
    public RestClient authServiceRestClient(
            RestClient.Builder builder,
            @Value("${app.auth-service.url}") String serviceUrl
    ) {
        return builder.baseUrl(serviceUrl).build();
    }

    @Bean
    public AuthSessionVerifier authSessionVerifier(
            RestClient authServiceRestClient,
            @Value("${app.auth-service.api-key}") String internalApiKey
    ) {
        return new HttpAuthSessionVerifier(authServiceRestClient, internalApiKey);
    }
}
