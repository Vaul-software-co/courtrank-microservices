package com.courtrank.authService.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cookies")
public record CookieProperties(
        boolean secure,
        String sameSite,
        String domain,
        long accessMaxAgeSeconds,
        long refreshMaxAgeSeconds,
        long resetMaxAgeSeconds
) {
    public boolean hasDomain() {
        return this.domain != null && !this.domain.isBlank();
    }
}
