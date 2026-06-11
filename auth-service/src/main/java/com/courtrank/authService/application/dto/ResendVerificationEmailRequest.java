package com.courtrank.authService.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationEmailRequest(
        @NotBlank
        @Email
        String email,

        String lang
) {
}
