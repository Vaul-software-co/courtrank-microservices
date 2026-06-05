package com.example.userService.infrastructure.security;

import com.example.userService.application.ports.security.AuthSessionVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

public class HttpAuthSessionVerifier implements AuthSessionVerifier {
    private static final Logger logger = LoggerFactory.getLogger(HttpAuthSessionVerifier.class);
    private static final String INTERNAL_API_KEY_HEADER = "x-internal-api-key";

    private final RestClient restClient;
    private final String internalApiKey;

    public HttpAuthSessionVerifier(RestClient restClient, String internalApiKey) {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw new IllegalStateException("AUTH_SERVICE_API_KEY or INTERNAL_API_KEY must be configured");
        }

        this.restClient = restClient;
        this.internalApiKey = internalApiKey;
    }

    @Override
    public boolean isActive(UUID sessionId) {
        try {
            SessionActiveResponse response = this.restClient
                    .get()
                    .uri("/auth/sessions/{sessionId}/active", sessionId)
                    .header(INTERNAL_API_KEY_HEADER, this.internalApiKey)
                    .retrieve()
                    .body(SessionActiveResponse.class);

            return response != null && response.active();
        } catch (RestClientException exception) {
            logger.warn("Auth session verification failed for sessionId={}: {}", sessionId, exception.getMessage());
            logger.debug("Auth session verification failure details", exception);
            return false;
        }
    }

    private record SessionActiveResponse(boolean active) {
    }
}
