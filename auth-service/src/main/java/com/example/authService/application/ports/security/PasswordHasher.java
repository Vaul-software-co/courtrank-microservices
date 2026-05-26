package com.example.authService.application.ports.security;

public interface PasswordHasher {
    String hashPassword(String password);
    boolean checkPassword(String password, String passwordHash);
}
