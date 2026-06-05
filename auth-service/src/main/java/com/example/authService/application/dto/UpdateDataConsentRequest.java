package com.example.authService.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateDataConsentRequest(
        @NotNull
        UUID userId,

        boolean accept
) {
}
