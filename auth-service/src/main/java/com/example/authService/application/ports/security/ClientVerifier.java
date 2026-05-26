package com.example.authService.application.ports.security;

public interface ClientVerifier {
    ApiClient verify(String apiKey);
}
