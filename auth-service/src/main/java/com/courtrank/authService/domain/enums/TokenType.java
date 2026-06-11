package com.courtrank.authService.domain.enums;

import java.time.Duration;

public enum TokenType {
    ACCESS(Duration.ofMinutes(15)),
    REFRESH(Duration.ofDays(7)),
    PASSWORD_RESET(Duration.ofMinutes(5));

    private final Duration expiration;

    TokenType(Duration expiration) {
        this.expiration = expiration;
    }

    public Duration getExpiration() {
        return expiration;
    }
}
