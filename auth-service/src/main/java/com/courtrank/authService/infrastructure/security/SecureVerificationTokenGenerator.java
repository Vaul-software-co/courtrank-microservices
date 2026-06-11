package com.courtrank.authService.infrastructure.security;

import com.courtrank.authService.application.ports.security.VerificationTokenGenerator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public class SecureVerificationTokenGenerator implements VerificationTokenGenerator {
    private static final int OTP_BOUND = 1_000_000;
    private static final int URL_TOKEN_BYTES = 32;

    private final SecureRandom secureRandom;

    public SecureVerificationTokenGenerator() {
        this.secureRandom = new SecureRandom();
    }

    @Override
    public String generateOtp() {
        return String.format("%06d", this.secureRandom.nextInt(OTP_BOUND));
    }

    @Override
    public String generateUrlToken() {
        byte[] bytes = new byte[URL_TOKEN_BYTES];
        this.secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    @Override
    public boolean compare(String token, String hashedToken) {
        return MessageDigest.isEqual(
                this.hash(token).getBytes(StandardCharsets.UTF_8),
                hashedToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}
