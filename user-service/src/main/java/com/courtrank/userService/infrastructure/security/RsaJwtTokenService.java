package com.courtrank.userService.infrastructure.security;

import com.courtrank.userService.application.ports.security.TokenService;
import com.courtrank.userService.domain.exceptions.InvalidCredentialsException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

public class RsaJwtTokenService implements TokenService {
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private final PublicKey publicKey;
    private final ObjectMapper objectMapper;
    private final Base64.Decoder decoder;

    public RsaJwtTokenService(PublicKey publicKey, ObjectMapper objectMapper) {
        this.publicKey = publicKey;
        this.objectMapper = objectMapper;
        this.decoder = Base64.getUrlDecoder();
    }

    @Override
    public boolean verifyAccess(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String unsignedToken = parts[0] + "." + parts[1];
            byte[] signature = this.decoder.decode(parts[2]);

            if (!this.verifySignature(unsignedToken, signature)) {
                return false;
            }

            Map<String, Object> payload = this.parsePayload(token);
            if (!this.isAccessToken(payload)) {
                return false;
            }

            long expiresAt = ((Number) payload.get("exp")).longValue();
            return expiresAt > Instant.now().getEpochSecond();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public UUID getTokenId(String token) {
        Map<String, Object> payload = this.parsePayload(token);
        return UUID.fromString((String) payload.get("id"));
    }

    @Override
    public UUID getSessionId(String token) {
        Map<String, Object> payload = this.parsePayload(token);
        Object sessionId = payload.get("sessionId");

        if (!(sessionId instanceof String value)) {
            throw new InvalidCredentialsException();
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidCredentialsException();
        }
    }

    private boolean isAccessToken(Map<String, Object> payload) {
        Object tokenType = payload.get("tokenType");
        if (tokenType instanceof String value) {
            return "access".equals(value);
        }

        Object legacyType = payload.get("type");
        return legacyType instanceof String value && "ACCESS".equals(value);
    }

    private Map<String, Object> parsePayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new InvalidCredentialsException();
            }

            String payload = new String(this.decoder.decode(parts[1]), StandardCharsets.UTF_8);
            return this.objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new InvalidCredentialsException();
        }
    }

    private boolean verifySignature(String unsignedToken, byte[] tokenSignature) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(this.publicKey);
            signature.update(unsignedToken.getBytes(StandardCharsets.UTF_8));
            return signature.verify(tokenSignature);
        } catch (Exception exception) {
            return false;
        }
    }
}
