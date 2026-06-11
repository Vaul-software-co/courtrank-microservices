package com.courtrank.authService.application.ports.user;

import java.util.UUID;

public interface UsernameAvailabilityVerifier {
    void assertAvailable(String username, UUID userId);
}
