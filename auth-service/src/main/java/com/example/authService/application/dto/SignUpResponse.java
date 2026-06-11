package com.example.authService.application.dto;

import com.example.authService.domain.entity.Authentication;

import java.util.Optional;

public record SignUpResponse(
        Authentication authentication,
        Optional<AuthResponse> auth
) {
}
