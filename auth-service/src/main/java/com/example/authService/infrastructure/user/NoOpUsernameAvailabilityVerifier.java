package com.example.authService.infrastructure.user;

import com.example.authService.application.ports.user.UsernameAvailabilityVerifier;

import java.util.UUID;

public class NoOpUsernameAvailabilityVerifier implements UsernameAvailabilityVerifier {
    @Override
    public void assertAvailable(String username, UUID userId) {
    }
}
