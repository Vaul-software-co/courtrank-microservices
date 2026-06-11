package com.courtrank.authService.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RevokeAllSessionsRequest(
        @NotNull
        UUID userId
) {
}
