package com.example.authService.infrastructure.security;

import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RsaPemKeyLoader {
    public PrivateKey loadPrivateKey(Resource resource) {
        try {
            String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String key = this.cleanPem(
                    pem,
                    "-----BEGIN PRIVATE KEY-----",
                    "-----END PRIVATE KEY-----"
            );

            byte[] decoded = Base64.getDecoder().decode(key);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load RSA private key", exception);
        }
    }

    public PublicKey loadPublicKey(Resource resource) {
        try {
            String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String key = this.cleanPem(
                    pem,
                    "-----BEGIN PUBLIC KEY-----",
                    "-----END PUBLIC KEY-----"
            );

            byte[] decoded = Base64.getDecoder().decode(key);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load RSA public key", exception);
        }
    }

    private String cleanPem(String pem, String begin, String end) {
        return pem
                .replace(begin, "")
                .replace(end, "")
                .replaceAll("\\s", "");
    }
}
