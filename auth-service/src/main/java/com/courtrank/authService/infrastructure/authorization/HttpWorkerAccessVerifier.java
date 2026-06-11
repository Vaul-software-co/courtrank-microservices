package com.courtrank.authService.infrastructure.authorization;

import com.courtrank.authService.application.ports.authorization.WorkerAccess;
import com.courtrank.authService.application.ports.authorization.WorkerAccessVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

public class HttpWorkerAccessVerifier implements WorkerAccessVerifier {
    private static final Logger logger = LoggerFactory.getLogger(HttpWorkerAccessVerifier.class);
    private static final String INTERNAL_API_KEY_HEADER = "x-internal-api-key";

    private final RestClient restClient;
    private final String internalApiKey;

    public HttpWorkerAccessVerifier(
            RestClient restClient,
            String internalApiKey
    ) {
        this.restClient = restClient;
        this.internalApiKey = internalApiKey;
    }

    @Override
    public WorkerAccess verify(UUID userId) {
        try {
            WorkerAccessResponse response = this.restClient
                    .get()
                    .uri("/users/{userId}/worker-access", userId)
                    .header(INTERNAL_API_KEY_HEADER, this.internalApiKey)
                    .retrieve()
                    .body(WorkerAccessResponse.class);

            if (response == null) {
                return WorkerAccess.denied();
            }

            return new WorkerAccess(response.hasAccess(), response.defaultClubId());
        } catch (RestClientException exception) {
            logger.warn("Worker access verification failed for userId={}: {}", userId, exception.getMessage());
            logger.debug("Worker access verification failure details", exception);
            return WorkerAccess.denied();
        }
    }

    private record WorkerAccessResponse(
            boolean hasAccess,
            UUID defaultClubId
    ) {
    }
}
