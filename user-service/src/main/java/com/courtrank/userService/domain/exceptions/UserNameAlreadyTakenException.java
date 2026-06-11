package com.courtrank.userService.domain.exceptions;

public class UserNameAlreadyTakenException extends RuntimeException {
    public UserNameAlreadyTakenException() {
        super("Username already taken");
    }
}
