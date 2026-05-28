package com.example.authService.infrastructure.user;

import com.example.authService.application.ports.user.UsernameAvailabilityVerifier;
import com.example.authService.domain.exceptions.ConflictException;
import com.example.authService.domain.exceptions.UserServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

public class HttpUsernameAvailabilityVerifier implements UsernameAvailabilityVerifier {
    private static final Logger logger = LoggerFactory.getLogger(HttpUsernameAvailabilityVerifier.class);
    private static final String INTERNAL_API_KEY_HEADER = "x-internal-api-key";

    private final RestClient restClient;
    private final String internalApiKey;

    public HttpUsernameAvailabilityVerifier(RestClient restClient, String internalApiKey) {
        this.restClient = restClient;
        this.internalApiKey = internalApiKey;
    }

    @Override
    public void assertAvailable(String username, UUID userId) {
        try {
            UsernameAvailabilityResponse response = this.restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users/username-available")
                            .queryParam("username", username)
                            .queryParam("userId", userId)
                            .build()
                    )
                    .header(INTERNAL_API_KEY_HEADER, this.internalApiKey)
                    .retrieve()
                    .body(UsernameAvailabilityResponse.class);

            if (response == null || !response.available()) {
                throw new ConflictException("Username already taken");
            }
        } catch (ConflictException exception) {
            throw exception;
        } catch (HttpClientErrorException.Conflict exception) {
            throw new ConflictException("Username already taken");
        } catch (RestClientException exception) {
            logger.warn("Username availability verification failed for username={}: {}", username, exception.getMessage());
            logger.debug("Username availability verification failure details", exception);
            throw new UserServiceUnavailableException();
        }
    }

    private record UsernameAvailabilityResponse(boolean available) {
    }
}
