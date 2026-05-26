package com.example.authService.unit.infrastructure.security;

import com.example.authService.domain.enums.TokenType;
import com.example.authService.domain.enums.UserRole;
import com.example.authService.domain.exceptions.InvalidCredentialsException;
import com.example.authService.infrastructure.security.RsaJwtTokenService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RsaJwtTokenServiceTest {
    @Test
    void generateToken_shouldCreateVerifiableRefreshToken() throws Exception {
        KeyPair keyPair = this.generateKeyPair();
        RsaJwtTokenService service = this.createService(keyPair);
        UUID userId = UUID.randomUUID();

        String token = service.generateToken(userId, TokenType.REFRESH);

        assertTrue(service.verifyRefresh(token));
        assertEquals(userId, service.getTokenId(token));
    }

    @Test
    void generateToken_shouldIncludeUserTypeClaimForAccessToken() throws Exception {
        KeyPair keyPair = this.generateKeyPair();
        RsaJwtTokenService service = this.createService(keyPair);
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        String token = service.generateAccessToken(userId, sessionId, UserRole.SUPER_ADMIN);
        Map<String, Object> payload = this.parsePayload(token);

        assertTrue(service.verifyAccess(token));
        assertEquals(sessionId, service.getSessionId(token));
        assertEquals(userId.toString(), payload.get("id"));
        assertEquals(sessionId.toString(), payload.get("sessionId"));
        assertEquals("SUPER_ADMIN", payload.get("type"));
        assertEquals("access", payload.get("tokenType"));
    }

    @Test
    void getSessionId_shouldThrowWhenAccessTokenDoesNotContainSessionId() throws Exception {
        RsaJwtTokenService service = this.createService(this.generateKeyPair());

        String token = service.generateToken(UUID.randomUUID(), TokenType.ACCESS, UserRole.MEMBER);

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.getSessionId(token)
        );
    }

    @Test
    void generatePasswordResetToken_shouldIncludeJti() throws Exception {
        RsaJwtTokenService service = this.createService(this.generateKeyPair());
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();

        String token = service.generatePasswordResetToken(userId, tokenId);
        Map<String, Object> payload = this.parsePayload(token);

        assertTrue(service.verifyPasswordReset(token));
        assertEquals(userId, service.getTokenId(token));
        assertEquals(tokenId, service.getTokenJti(token));
        assertEquals(tokenId.toString(), payload.get("jti"));
        assertEquals("password_reset", payload.get("tokenType"));
    }

    @Test
    void getTokenJti_shouldThrowWhenTokenDoesNotContainJti() throws Exception {
        RsaJwtTokenService service = this.createService(this.generateKeyPair());

        String token = service.generateToken(UUID.randomUUID(), TokenType.PASSWORD_RESET);

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.getTokenJti(token)
        );
    }

    @Test
    void verifyAccess_shouldReturnFalseWhenTokenTypeIsRefresh() throws Exception {
        RsaJwtTokenService service = this.createService(this.generateKeyPair());

        String token = service.generateToken(UUID.randomUUID(), TokenType.REFRESH);

        assertFalse(service.verifyAccess(token));
    }

    @Test
    void verifyRefresh_shouldReturnFalseWhenSignatureWasTampered() throws Exception {
        RsaJwtTokenService service = this.createService(this.generateKeyPair());
        String token = service.generateToken(UUID.randomUUID(), TokenType.REFRESH);
        String tamperedToken = token.substring(0, token.length() - 2) + "xx";

        assertFalse(service.verifyRefresh(tamperedToken));
    }

    @Test
    void verifyRefresh_shouldSupportTypeClaimForLegacyTokens() throws Exception {
        KeyPair keyPair = this.generateKeyPair();
        RsaJwtTokenService service = this.createService(keyPair);
        UUID userId = UUID.randomUUID();
        String legacyToken = this.createLegacyRefreshToken(keyPair, userId);

        assertTrue(service.verifyRefresh(legacyToken));
        assertEquals(userId, service.getTokenId(legacyToken));
    }

    @Test
    void getTokenId_shouldThrowWhenTokenIsMalformed() throws Exception {
        RsaJwtTokenService service = this.createService(this.generateKeyPair());

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.getTokenId("malformed-token")
        );
    }

    private RsaJwtTokenService createService(KeyPair keyPair) {
        return new RsaJwtTokenService(
                keyPair.getPrivate(),
                keyPair.getPublic(),
                new ObjectMapper()
        );
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes());
    }

    private Map<String, Object> parsePayload(String token) throws Exception {
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        return new ObjectMapper().readValue(payload, new TypeReference<>() {});
    }

    private String createLegacyRefreshToken(KeyPair keyPair, UUID userId) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "RS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", userId.toString());
        payload.put("type", "REFRESH");
        payload.put("iat", 1);
        payload.put("exp", 4_102_444_800L);

        String unsignedToken = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(header))
                + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(payload));

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(unsignedToken.getBytes());

        return unsignedToken + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
    }
}
