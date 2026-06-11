package com.courtrank.authService.application.dto;

import jakarta.validation.constraints.NotBlank;
public record VerifyPasswordOtpRequest(
        @NotBlank
        String email,

        @NotBlank
        String otp
) {
}
