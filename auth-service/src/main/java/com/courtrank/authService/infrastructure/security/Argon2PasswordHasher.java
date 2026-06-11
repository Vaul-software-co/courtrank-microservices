package com.courtrank.authService.infrastructure.security;

import com.courtrank.authService.application.ports.security.PasswordHasher;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class Argon2PasswordHasher implements PasswordHasher {
    private final PasswordEncoder passwordEncoder;
    private final String pepper;

    public Argon2PasswordHasher(String pepper) {
        this.passwordEncoder = new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
        this.pepper = pepper;
    }

    @Override
    public String hashPassword(String password) {
        return this.passwordEncoder.encode(this.applyPepper(password));
    }

    @Override
    public boolean checkPassword(String password, String passwordHash) {
        return this.passwordEncoder.matches(this.applyPepper(password), passwordHash);
    }

    private String applyPepper(String password) {
        return password + this.pepper;
    }
}
