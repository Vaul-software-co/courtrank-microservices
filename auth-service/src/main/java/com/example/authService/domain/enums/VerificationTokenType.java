package com.example.authService.domain.enums;

import java.time.Duration;

public enum VerificationTokenType {
    EMAIL_VERIFICATION(Duration.ofHours(24)),
    PASSWORD_RESET(Duration.ofMinutes(15)),
    PASSWORD_RESET_CONFIRMATION(Duration.ofMinutes(5));

    private final Duration expiration;

    VerificationTokenType(Duration expiration) {
        this.expiration = expiration;
    }

    public Duration getExpiration() {
        return expiration;
    }
}
