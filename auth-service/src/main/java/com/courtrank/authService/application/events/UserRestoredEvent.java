package com.courtrank.authService.application.events;

import com.courtrank.authService.domain.enums.UserRole;

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
