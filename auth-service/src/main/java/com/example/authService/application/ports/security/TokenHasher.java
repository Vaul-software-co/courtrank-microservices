package com.example.authService.application.ports.security;

public interface TokenHasher {
    String hash(String token);
}
