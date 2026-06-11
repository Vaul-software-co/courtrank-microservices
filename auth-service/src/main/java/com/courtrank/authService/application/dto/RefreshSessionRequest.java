package com.courtrank.authService.application.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshSessionRequest(
        @NotBlank
        String refreshToken,

        HttpContext http
) {
}
