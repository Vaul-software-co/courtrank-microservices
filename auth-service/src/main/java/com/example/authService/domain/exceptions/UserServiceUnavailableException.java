package com.example.authService.domain.exceptions;

public class UserServiceUnavailableException extends RuntimeException {
    public UserServiceUnavailableException() {
        super("User service unavailable");
    }
}
