package com.example.userService.unit.infrastructure.security;

import com.example.userService.domain.exceptions.InvalidCredentialsException;
import com.example.userService.infrastructure.security.RsaJwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RsaJwtTokenServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private KeyPair keyPair;
    private RsaJwtTokenService tokenService;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        this.keyPair = generator.generateKeyPair();
        this.tokenService = new RsaJwtTokenService(this.keyPair.getPublic(), this.objectMapper);
    }

    @Test
    void verifyAccess_shouldAcceptSignedAccessToken() throws Exception {
        String token = this.token(Map.of(
                "id", UUID.randomUUID().toString(),
                "sessionId", UUID.randomUUID().toString(),
                "tokenType", "access",
                "exp", Instant.now().plusSeconds(60).getEpochSecond()
        ), this.keyPair.getPrivate());

        assertThat(this.tokenService.verifyAccess(token)).isTrue();
    }

    @Test
    void verifyAccess_shouldRejectExpiredAccessToken() throws Exception {
        String token = this.token(Map.of(
                "id", UUID.randomUUID().toString(),
                "sessionId", UUID.randomUUID().toString(),
                "tokenType", "access",
                "exp", Instant.now().minusSeconds(60).getEpochSecond()
        ), this.keyPair.getPrivate());

        assertThat(this.tokenService.verifyAccess(token)).isFalse();
    }

    @Test
    void verifyAccess_shouldRejectTokenSignedWithAnotherKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair otherKeyPair = generator.generateKeyPair();

        String token = this.token(Map.of(
                "id", UUID.randomUUID().toString(),
                "sessionId", UUID.randomUUID().toString(),
                "tokenType", "access",
                "exp", Instant.now().plusSeconds(60).getEpochSecond()
        ), otherKeyPair.getPrivate());

        assertThat(this.tokenService.verifyAccess(token)).isFalse();
    }

    @Test
    void verifyAccess_shouldRejectRefreshToken() throws Exception {
        String token = this.token(Map.of(
                "id", UUID.randomUUID().toString(),
                "sessionId", UUID.randomUUID().toString(),
                "tokenType", "refresh",
                "exp", Instant.now().plusSeconds(60).getEpochSecond()
        ), this.keyPair.getPrivate());

        assertThat(this.tokenService.verifyAccess(token)).isFalse();
    }

    @Test
    void getSessionId_shouldReturnSessionIdFromPayload() throws Exception {
        UUID sessionId = UUID.randomUUID();
        String token = this.token(Map.of(
                "id", UUID.randomUUID().toString(),
                "sessionId", sessionId.toString(),
                "tokenType", "access",
                "exp", Instant.now().plusSeconds(60).getEpochSecond()
        ), this.keyPair.getPrivate());

        assertThat(this.tokenService.getSessionId(token)).isEqualTo(sessionId);
    }

    @Test
    void getSessionId_shouldRejectMissingSessionId() throws Exception {
        String token = this.token(Map.of(
                "id", UUID.randomUUID().toString(),
                "tokenType", "access",
                "exp", Instant.now().plusSeconds(60).getEpochSecond()
        ), this.keyPair.getPrivate());

        assertThatThrownBy(() -> this.tokenService.getSessionId(token))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    private String token(Map<String, Object> payload, PrivateKey privateKey) throws Exception {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString(this.objectMapper.writeValueAsBytes(Map.of(
                "alg", "RS256",
                "typ", "JWT"
        )));
        String body = encoder.encodeToString(this.objectMapper.writeValueAsBytes(payload));
        String unsignedToken = header + "." + body;

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(unsignedToken.getBytes(StandardCharsets.UTF_8));

        return unsignedToken + "." + encoder.encodeToString(signature.sign());
    }
}
