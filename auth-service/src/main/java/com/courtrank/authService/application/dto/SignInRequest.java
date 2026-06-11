package com.courtrank.authService.application.dto;
import jakarta.validation.constraints.*;

public record SignInRequest(
    @Email
    @NotBlank
    String email,

    @NotBlank
    String password
) {
}
