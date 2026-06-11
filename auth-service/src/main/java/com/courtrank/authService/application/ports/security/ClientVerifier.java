package com.courtrank.authService.application.ports.security;

public interface ClientVerifier {
    ApiClient verify(String apiKey);
}
