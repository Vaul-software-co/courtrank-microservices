package com.example.authService.unit.domain.service;

import com.example.authService.domain.exceptions.WeakPasswordException;
import com.example.authService.domain.service.PasswordPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {
    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    @Test
    void validate_shouldAcceptStrongPassword() {
        assertDoesNotThrow(() -> this.passwordPolicy.validate("StrongPass1!"));
    }

    @Test
    void validate_shouldRejectPasswordShorterThanEightCharacters() {
        assertThrows(WeakPasswordException.class, () -> this.passwordPolicy.validate("Aa1!"));
    }

    @Test
    void validate_shouldRejectPasswordLongerThanSixtyFourCharacters() {
        String password = "A1!" + "a".repeat(62);

        assertThrows(WeakPasswordException.class, () -> this.passwordPolicy.validate(password));
    }

    @Test
    void validate_shouldRejectPasswordWithoutUppercaseLetter() {
        assertThrows(WeakPasswordException.class, () -> this.passwordPolicy.validate("strongpass1!"));
    }

    @Test
    void validate_shouldRejectPasswordWithoutNumber() {
        assertThrows(WeakPasswordException.class, () -> this.passwordPolicy.validate("StrongPass!"));
    }

    @Test
    void validate_shouldRejectPasswordWithoutSpecialCharacter() {
        assertThrows(WeakPasswordException.class, () -> this.passwordPolicy.validate("StrongPass1"));
    }
}