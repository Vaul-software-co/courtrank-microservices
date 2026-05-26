package com.example.authService.application.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionSummary(
        UUID id,
        String client,
        String ip,
        String userAgent,
        Instant createdAt,
        Instant expiresAt
) {
}
