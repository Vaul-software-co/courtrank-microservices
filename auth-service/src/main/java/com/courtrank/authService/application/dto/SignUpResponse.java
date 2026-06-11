package com.courtrank.authService.application.dto;

import com.courtrank.authService.domain.entity.Authentication;

import java.util.Optional;

public record SignUpResponse(
        Authentication authentication,
        Optional<AuthResponse> auth
) {
}
