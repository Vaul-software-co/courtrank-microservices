package com.courtrank.authService.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank
        String resetToken,

        @NotBlank
        String newPassword
) {
}
