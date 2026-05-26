package com.example.authService.unit.infrastructure.security;

import com.example.authService.infrastructure.security.Argon2PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Argon2PasswordHasherTest {
    @Test
    void hashPassword_shouldGenerateArgon2idHash() {
        Argon2PasswordHasher hasher = new Argon2PasswordHasher("pepper");

        String hash = hasher.hashPassword("StrongPass1!");

        assertTrue(hash.startsWith("$argon2id$"));
        assertTrue(hash.contains("m=65536,t=3,p=1"));
    }

    @Test
    void hashPassword_shouldUseDifferentSaltForSamePassword() {
        Argon2PasswordHasher hasher = new Argon2PasswordHasher("pepper");

        String firstHash = hasher.hashPassword("StrongPass1!");
        String secondHash = hasher.hashPassword("StrongPass1!");

        assertNotEquals(firstHash, secondHash);
    }

    @Test
    void checkPassword_shouldReturnTrueWhenPasswordAndPepperMatch() {
        Argon2PasswordHasher hasher = new Argon2PasswordHasher("pepper");
        String hash = hasher.hashPassword("StrongPass1!");

        assertTrue(hasher.checkPassword("StrongPass1!", hash));
    }

    @Test
    void checkPassword_shouldReturnFalseWhenPasswordDoesNotMatch() {
        Argon2PasswordHasher hasher = new Argon2PasswordHasher("pepper");
        String hash = hasher.hashPassword("StrongPass1!");

        assertFalse(hasher.checkPassword("WrongPass1!", hash));
    }

    @Test
    void checkPassword_shouldReturnFalseWhenPepperDoesNotMatch() {
        Argon2PasswordHasher originalHasher = new Argon2PasswordHasher("pepper");
        Argon2PasswordHasher differentPepperHasher = new Argon2PasswordHasher("other-pepper");
        String hash = originalHasher.hashPassword("StrongPass1!");

        assertFalse(differentPepperHasher.checkPassword("StrongPass1!", hash));
    }
}
