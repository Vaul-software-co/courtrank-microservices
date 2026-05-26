package com.example.authService.infrastructure.config;

import com.example.authService.application.ports.security.ClientVerifier;
import com.example.authService.application.ports.security.PasswordHasher;
import com.example.authService.application.ports.security.TokenHasher;
import com.example.authService.application.ports.security.TokenService;
import com.example.authService.application.ports.security.VerificationTokenGenerator;
import com.example.authService.domain.service.PasswordPolicy;
import com.example.authService.infrastructure.security.Argon2PasswordHasher;
import com.example.authService.infrastructure.security.EnvironmentClientVerifier;
import com.example.authService.infrastructure.security.RsaJwtTokenService;
import com.example.authService.infrastructure.security.RsaPemKeyLoader;
import com.example.authService.infrastructure.security.SecureVerificationTokenGenerator;
import com.example.authService.infrastructure.security.Sha256TokenHasher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.security.PrivateKey;
import java.security.PublicKey;

@Configuration
public class SecurityAdapterConfig {
    @Bean
    public PasswordPolicy passwordPolicy() {
        return new PasswordPolicy();
    }

    @Bean
    public ClientVerifier clientVerifier(
            @Value("${app.api-keys.web}") String webApiKey,
            @Value("${app.api-keys.mobile}") String mobileApiKey
    ) {
        return new EnvironmentClientVerifier(webApiKey, mobileApiKey);
    }

    @Bean
    public PasswordHasher passwordHasher(
            @Value("${app.password.pepper}") String passwordPepper
    ) {
        return new Argon2PasswordHasher(passwordPepper);
    }

    @Bean
    public TokenHasher tokenHasher() {
        return new Sha256TokenHasher();
    }

    @Bean
    public VerificationTokenGenerator verificationTokenGenerator() {
        return new SecureVerificationTokenGenerator();
    }

    @Bean
    public RsaPemKeyLoader rsaPemKeyLoader() {
        return new RsaPemKeyLoader();
    }

    @Bean
    public PrivateKey jwtPrivateKey(
            RsaPemKeyLoader keyLoader,
            @Value("${app.jwt.private-key}") Resource privateKeyResource
    ) {
        return keyLoader.loadPrivateKey(privateKeyResource);
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
            PrivateKey jwtPrivateKey,
            PublicKey jwtPublicKey,
            ObjectMapper objectMapper
    ) {
        return new RsaJwtTokenService(jwtPrivateKey, jwtPublicKey, objectMapper);
    }
}
