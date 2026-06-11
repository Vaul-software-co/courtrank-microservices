package com.courtrank.userService.unit.infrastructure.security;

import com.courtrank.userService.infrastructure.security.RsaPemKeyLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RsaPemKeyLoaderTest {
    @Mock
    Resource resource;

    private final RsaPemKeyLoader loader = new RsaPemKeyLoader();

    @Test
    void loadPublicKey_shouldLoadPemEncodedRsaPublicKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(keyPair.getPublic().getEncoded());
        String pem = "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----";
        when(this.resource.getInputStream())
                .thenReturn(new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));

        var publicKey = this.loader.loadPublicKey(this.resource);

        assertThat(publicKey.getAlgorithm()).isEqualTo("RSA");
        assertThat(publicKey.getEncoded()).isEqualTo(keyPair.getPublic().getEncoded());
    }

    @Test
    void loadPublicKey_shouldWrapInvalidPem() throws Exception {
        when(this.resource.getInputStream())
                .thenReturn(new ByteArrayInputStream("not-a-key".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> this.loader.loadPublicKey(this.resource))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not load RSA public key");
    }
}
