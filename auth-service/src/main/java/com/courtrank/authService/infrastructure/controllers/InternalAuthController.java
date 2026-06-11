package com.courtrank.authService.infrastructure.controllers;

import com.courtrank.authService.application.dto.SessionActiveResponse;
import com.courtrank.authService.domain.entity.Session;
import com.courtrank.authService.domain.repository.SessionRepository;
import com.courtrank.authService.infrastructure.security.InternalApiKeyVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/auth")
public class InternalAuthController {
    private static final String INTERNAL_API_KEY_HEADER = "x-internal-api-key";

    private final SessionRepository sessionRepository;
    private final InternalApiKeyVerifier internalApiKeyVerifier;

    public InternalAuthController(
            SessionRepository sessionRepository,
            @Value("${app.internal-api-key}") String internalApiKey
    ) {
        this.sessionRepository = sessionRepository;
        this.internalApiKeyVerifier = new InternalApiKeyVerifier(internalApiKey);
    }

    @GetMapping("/sessions/{sessionId}/active")
    public SessionActiveResponse sessionActive(
            @RequestHeader(INTERNAL_API_KEY_HEADER) String apiKey,
            @PathVariable UUID sessionId
    ) {
        this.internalApiKeyVerifier.verify(apiKey);

        boolean active = this.sessionRepository.findById(sessionId)
                .filter(Session::isActive)
                .isPresent();

        return new SessionActiveResponse(active);
    }
}
