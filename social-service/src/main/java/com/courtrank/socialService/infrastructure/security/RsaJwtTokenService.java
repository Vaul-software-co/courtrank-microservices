package com.courtrank.socialService.infrastructure.security;

import com.courtrank.socialService.application.ports.security.TokenService;
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
    private final Base64.Decoder decoder = Base64.getUrlDecoder();

    public RsaJwtTokenService(PublicKey publicKey, ObjectMapper objectMapper) {
        this.publicKey = publicKey;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean verifyAccess(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String unsignedToken = parts[0] + "." + parts[1];
            if (!this.verifySignature(unsignedToken, this.decoder.decode(parts[2]))) {
                return false;
            }

            Map<String, Object> payload = this.parsePayload(token);
            long expiresAt = ((Number) payload.get("exp")).longValue();
            return this.isAccessToken(payload) && expiresAt > Instant.now().getEpochSecond();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override public UUID getTokenId(String token) { return UUID.fromString((String) this.parsePayload(token).get("id")); }
    @Override public UUID getSessionId(String token) { return UUID.fromString((String) this.parsePayload(token).get("sessionId")); }

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
                throw new IllegalArgumentException("Invalid token");
            }

            String payload = new String(this.decoder.decode(parts[1]), StandardCharsets.UTF_8);
            return this.objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid token", exception);
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
