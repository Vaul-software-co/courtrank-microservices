package com.courtrank.socialService.infrastructure.user;

import com.courtrank.socialService.application.dto.SocialUserSnapshot;
import com.courtrank.socialService.application.ports.SocialUserProfileProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class HttpSocialUserProfileProvider implements SocialUserProfileProvider {
    private static final Logger logger = LoggerFactory.getLogger(HttpSocialUserProfileProvider.class);
    private static final String INTERNAL_API_KEY_HEADER = "x-internal-api-key";

    private final RestClient restClient;
    private final String internalApiKey;

    public HttpSocialUserProfileProvider(RestClient restClient, String internalApiKey) {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw new IllegalStateException("USER_SERVICE_API_KEY or INTERNAL_API_KEY must be configured");
        }

        this.restClient = restClient;
        this.internalApiKey = internalApiKey;
    }

    @Override
    public Optional<SocialUserSnapshot> findByUserId(UUID userId) {
        try {
            InternalUserSummaryResponse response = this.restClient.get()
                    .uri("/internal/users/{id}/summary", userId)
                    .header(INTERNAL_API_KEY_HEADER, this.internalApiKey)
                    .retrieve()
                    .body(InternalUserSummaryResponse.class);

            if (response == null) {
                return Optional.empty();
            }

            boolean active = "VISIBLE".equals(response.status());
            return Optional.of(new SocialUserSnapshot(
                    response.id(),
                    response.name(),
                    response.username(),
                    response.avatarUrl(),
                    response.privateProfile(),
                    active,
                    active ? null : Instant.now(),
                    Instant.now()
            ));
        } catch (RestClientException exception) {
            logger.warn("User profile lookup failed for userId={}: {}", userId, exception.getMessage());
            return Optional.empty();
        }
    }

    private record InternalUserSummaryResponse(
            UUID id,
            String name,
            String username,
            String email,
            String avatarUrl,
            boolean privateProfile,
            String status
    ) {
    }
}
