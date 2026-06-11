package com.courtrank.authService.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VerificationEmailRequest(
        @NotNull
        UUID id,

        @NotBlank
        String email,

        String lang
) {
}
