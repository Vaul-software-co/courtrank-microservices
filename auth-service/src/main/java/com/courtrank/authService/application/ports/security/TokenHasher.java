package com.courtrank.authService.application.ports.security;

public interface TokenHasher {
    String hash(String token);
}
