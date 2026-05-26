package com.example.authService.domain.exceptions;

public class DisabledAccountException extends RuntimeException {
    public DisabledAccountException() {
        super("Account disabled");
    }
}
