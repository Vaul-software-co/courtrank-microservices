package com.example.authService.unit.infrastructure.security;

import com.example.authService.infrastructure.security.SecureVerificationTokenGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SecureVerificationTokenGeneratorTest {
    @Test
    void generateOtp_shouldReturnSixDigits() {
        SecureVerificationTokenGenerator generator = new SecureVerificationTokenGenerator();

        String otp = generator.generateOtp();

        assertTrue(otp.matches("\\d{6}"));
    }

    @Test
    void generateUrlToken_shouldReturnUrlSafeToken() {
        SecureVerificationTokenGenerator generator = new SecureVerificationTokenGenerator();

        String token = generator.generateUrlToken();

        assertFalse(token.isBlank());
        assertFalse(token.contains("+"));
        assertFalse(token.contains("/"));
        assertFalse(token.contains("="));
    }

    @Test
    void hash_shouldReturnSha256Hex() {
        SecureVerificationTokenGenerator generator = new SecureVerificationTokenGenerator();

        String hash = generator.hash("raw-token");

        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    void compare_shouldReturnTrueWhenTokenMatchesHash() {
        SecureVerificationTokenGenerator generator = new SecureVerificationTokenGenerator();
        String hash = generator.hash("raw-token");

        assertTrue(generator.compare("raw-token", hash));
    }

    @Test
    void compare_shouldReturnFalseWhenTokenDoesNotMatchHash() {
        SecureVerificationTokenGenerator generator = new SecureVerificationTokenGenerator();
        String hash = generator.hash("raw-token");

        assertFalse(generator.compare("other-token", hash));
    }

    @Test
    void generateUrlToken_shouldUseRandomValues() {
        SecureVerificationTokenGenerator generator = new SecureVerificationTokenGenerator();

        assertNotEquals(generator.generateUrlToken(), generator.generateUrlToken());
    }
}
