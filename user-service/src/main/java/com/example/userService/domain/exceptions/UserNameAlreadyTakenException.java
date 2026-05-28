package com.example.userService.domain.exceptions;

public class UserNameAlreadyTakenException extends RuntimeException {
    public UserNameAlreadyTakenException() {
        super("Username already taken");
    }
}
