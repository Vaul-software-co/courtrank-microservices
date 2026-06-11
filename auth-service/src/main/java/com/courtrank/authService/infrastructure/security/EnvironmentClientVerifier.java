package com.courtrank.authService.infrastructure.security;

import com.courtrank.authService.application.ports.security.ApiClient;
import com.courtrank.authService.application.ports.security.ClientVerifier;
import com.courtrank.authService.domain.enums.UserRole;
import com.courtrank.authService.domain.exceptions.InvalidCredentialsException;

public class EnvironmentClientVerifier implements ClientVerifier {
    private final String webApiKey;
    private final String mobileApiKey;

    public EnvironmentClientVerifier(String webApiKey, String mobileApiKey) {
        this.webApiKey = webApiKey;
        this.mobileApiKey = mobileApiKey;
    }

    @Override
    public ApiClient verify(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new InvalidCredentialsException();
        }

        if (apiKey.equals(this.webApiKey)) {
            return new ApiClient("web", UserRole.ADMIN);
        }

        if (apiKey.equals(this.mobileApiKey)) {
            return new ApiClient("mobile", UserRole.MEMBER);
        }

        throw new InvalidCredentialsException();
    }
}
