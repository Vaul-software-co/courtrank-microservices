package com.courtrank.authService.infrastructure.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class InternalApiKeyVerifier {
    private final String internalApiKey;

    public InternalApiKeyVerifier(String internalApiKey) {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw new IllegalStateException("INTERNAL_API_KEY must be configured for internal endpoints");
        }

        this.internalApiKey = internalApiKey;
    }

    public void verify(String apiKey) {
        if (apiKey == null || apiKey.isBlank() || !this.matches(apiKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API key");
        }
    }

    private boolean matches(String apiKey) {
        return MessageDigest.isEqual(
                apiKey.getBytes(StandardCharsets.UTF_8),
                this.internalApiKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}
