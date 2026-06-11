package com.example.authService.application.events;

import com.example.authService.domain.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserRestoredEvent(
        UUID id,
        String email,
        String name,
        String username,
        UserRole role,
        boolean emailVerified,
        String acceptedTermsVersion,
        boolean acceptedDataCommercialization,
        Instant occurredAt
) {
}
