package com.example.authService.domain.service;

import com.example.authService.domain.exceptions.WeakPasswordException;

public class PasswordPolicy {
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;

    public void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new WeakPasswordException("Password must be at least 8 characters long");
        }

        if (password.length() > MAX_LENGTH) {
            throw new WeakPasswordException("Password is too long");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new WeakPasswordException("Password must contain at least one uppercase letter");
        }

        if (!password.matches(".*[0-9].*")) {
            throw new WeakPasswordException("Password must contain at least one number");
        }

        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            throw new WeakPasswordException("Password must contain at least one special character");
        }
    }
}
