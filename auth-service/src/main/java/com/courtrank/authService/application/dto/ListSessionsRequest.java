package com.courtrank.authService.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ListSessionsRequest(
        @NotNull
        UUID userId
) {
}
