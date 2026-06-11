package com.courtrank.authService.unit.infrastructure.security;

import com.courtrank.authService.infrastructure.security.Sha256TokenHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class Sha256TokenHasherTest {
    @Test
    void hash_shouldReturnExpectedSha256Hex() {
        Sha256TokenHasher hasher = new Sha256TokenHasher();

        String hash = hasher.hash("refresh-token");

        assertEquals(
                "0eb17643d4e9261163783a420859c92c7d212fa9624106a12b510afbec266120",
                hash
        );
    }

    @Test
    void hash_shouldBeDeterministic() {
        Sha256TokenHasher hasher = new Sha256TokenHasher();

        assertEquals(hasher.hash("token"), hasher.hash("token"));
    }

    @Test
    void hash_shouldReturnDifferentHashesForDifferentTokens() {
        Sha256TokenHasher hasher = new Sha256TokenHasher();

        assertNotEquals(hasher.hash("token-a"), hasher.hash("token-b"));
    }
}
