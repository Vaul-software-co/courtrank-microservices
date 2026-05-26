package com.example.authService.unit.infrastructure.security;

import com.example.authService.infrastructure.security.RsaPemKeyLoader;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RsaPemKeyLoaderTest {
    @Test
    void loadPrivateKey_shouldLoadPkcs8Pem() throws Exception {
        KeyPair keyPair = this.generateKeyPair();
        RsaPemKeyLoader loader = new RsaPemKeyLoader();

        PrivateKey loaded = loader.loadPrivateKey(new ByteArrayResource(this.toPrivatePem(keyPair).getBytes(StandardCharsets.UTF_8)));

        assertEquals(keyPair.getPrivate(), loaded);
    }

    @Test
    void loadPublicKey_shouldLoadX509Pem() throws Exception {
        KeyPair keyPair = this.generateKeyPair();
        RsaPemKeyLoader loader = new RsaPemKeyLoader();

        PublicKey loaded = loader.loadPublicKey(new ByteArrayResource(this.toPublicPem(keyPair).getBytes(StandardCharsets.UTF_8)));

        assertEquals(keyPair.getPublic(), loaded);
    }

    @Test
    void loadPrivateKey_shouldThrowWhenPemIsInvalid() {
        RsaPemKeyLoader loader = new RsaPemKeyLoader();

        assertThrows(
                IllegalStateException.class,
                () -> loader.loadPrivateKey(new ByteArrayResource("invalid".getBytes(StandardCharsets.UTF_8)))
        );
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String toPrivatePem(KeyPair keyPair) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
    }

    private String toPublicPem(KeyPair keyPair) {
        return "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(keyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";
    }
}
