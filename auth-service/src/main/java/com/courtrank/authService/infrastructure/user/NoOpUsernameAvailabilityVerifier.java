package com.courtrank.authService.infrastructure.user;

import com.courtrank.authService.application.ports.user.UsernameAvailabilityVerifier;

import java.util.UUID;

public class NoOpUsernameAvailabilityVerifier implements UsernameAvailabilityVerifier {
    @Override
    public void assertAvailable(String username, UUID userId) {
    }
}
