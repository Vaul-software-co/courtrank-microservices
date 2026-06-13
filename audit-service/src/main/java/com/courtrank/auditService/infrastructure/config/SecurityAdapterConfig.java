package com.courtrank.auditService.infrastructure.config;

import com.courtrank.auditService.application.ports.security.TokenService;
import com.courtrank.auditService.infrastructure.security.RsaJwtTokenService;
import com.courtrank.auditService.infrastructure.security.RsaPemKeyLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

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
}
