package com.example.authService.application.dto;

import java.util.Optional;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Optional<UUID> clubId
) {
}
