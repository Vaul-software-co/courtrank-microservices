package com.courtrank.authService.domain.exceptions;

public class UserServiceUnavailableException extends RuntimeException {
    public UserServiceUnavailableException() {
        super("User service unavailable");
    }
}
