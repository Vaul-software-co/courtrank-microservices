package com.example.authService.infrastructure.security;

import com.example.authService.application.ports.security.TokenService;
import com.example.authService.domain.enums.TokenType;
import com.example.authService.domain.enums.UserRole;
import com.example.authService.domain.exceptions.InvalidCredentialsException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class RsaJwtTokenService implements TokenService {
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final ObjectMapper objectMapper;
    private final Base64.Encoder encoder;
    private final Base64.Decoder decoder;

    public RsaJwtTokenService(PrivateKey privateKey, PublicKey publicKey, ObjectMapper objectMapper) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.objectMapper = objectMapper;
        this.encoder = Base64.getUrlEncoder().withoutPadding();
        this.decoder = Base64.getUrlDecoder();
    }

    @Override
    public String generateToken(UUID id, TokenType type) {
        if (type == TokenType.REFRESH) {
            return this.generateSignedToken(id, type, null, UUID.randomUUID(), null);
        }

        return this.generateToken(id, type, null);
    }

    @Override
    public String generateToken(UUID id, TokenType type, UserRole userType) {
        return this.generateSignedToken(id, type, null, null, userType);
    }

    @Override
    public String generateAccessToken(UUID id, UUID sessionId, UserRole userType) {
        return this.generateSignedToken(id, TokenType.ACCESS, sessionId, null, userType);
    }

    @Override
    public String generatePasswordResetToken(UUID id, UUID tokenId) {
        return this.generateSignedToken(id, TokenType.PASSWORD_RESET, null, tokenId, null);
    }

    private String generateSignedToken(UUID id, TokenType type, UUID sessionId, UUID tokenId, UserRole userType) {
        Instant now = Instant.now();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "RS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id.toString());
        if (sessionId != null) {
            payload.put("sessionId", sessionId.toString());
        }
        if (tokenId != null) {
            payload.put("jti", tokenId.toString());
        }
        if (userType != null) {
            payload.put("type", userType.name());
        }
        payload.put("tokenType", this.toTokenTypeValue(type));
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plus(type.getExpiration()).getEpochSecond());

        String unsignedToken = this.encodeJson(header) + "." + this.encodeJson(payload);
        return unsignedToken + "." + this.sign(unsignedToken);
    }

    @Override
    public boolean verifyAccess(String token) {
        return this.verify(token, TokenType.ACCESS);
    }

    @Override
    public boolean verifyRefresh(String token) {
        return this.verify(token, TokenType.REFRESH);
    }

    @Override
    public boolean verifyPasswordReset(String token) {
        return this.verify(token, TokenType.PASSWORD_RESET);
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

    @Override
    public UUID getTokenJti(String token) {
        Map<String, Object> payload = this.parsePayload(token);
        Object jti = payload.get("jti");

        if (!(jti instanceof String value)) {
            throw new InvalidCredentialsException();
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidCredentialsException();
        }
    }

    private boolean verify(String token, TokenType expectedType) {
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
            if (!this.matchesType(payload, expectedType)) {
                return false;
            }

            long expiresAt = ((Number) payload.get("exp")).longValue();
            return expiresAt > Instant.now().getEpochSecond();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean matchesType(Map<String, Object> payload, TokenType expectedType) {
        Object tokenType = payload.get("tokenType");
        if (tokenType instanceof String value) {
            return this.toTokenTypeValue(expectedType).equals(value);
        }

        Object legacyType = payload.get("type");
        if (legacyType instanceof String value) {
            return expectedType.name().equals(value);
        }

        return false;
    }

    private String toTokenTypeValue(TokenType type) {
        return type.name().toLowerCase();
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

    private String encodeJson(Map<String, Object> value) {
        try {
            byte[] json = this.objectMapper.writeValueAsBytes(value);
            return this.encoder.encodeToString(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not encode JWT", exception);
        }
    }

    private String sign(String unsignedToken) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(this.privateKey);
            signature.update(unsignedToken.getBytes(StandardCharsets.UTF_8));
            return this.encoder.encodeToString(signature.sign());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign JWT", exception);
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
