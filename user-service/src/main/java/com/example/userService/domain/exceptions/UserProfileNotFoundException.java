package com.example.userService.domain.exceptions;

public class UserProfileNotFoundException extends RuntimeException {
    public UserProfileNotFoundException() {
        super("User profile not found");
    }
}
