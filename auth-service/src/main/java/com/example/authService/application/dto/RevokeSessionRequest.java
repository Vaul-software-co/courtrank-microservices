package com.example.authService.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RevokeSessionRequest(
        @NotNull
        UUID userId,

        @NotNull
        UUID sessionId
) {
}
