package com.example.authService.application.ports.security;

public interface VerificationTokenGenerator {
    String generateOtp();
    String generateUrlToken();
    String hash(String rawToken);
    boolean compare(String token, String hashedToken);
}
