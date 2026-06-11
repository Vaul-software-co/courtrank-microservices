package com.courtrank.userService.application.ports.security;

import java.util.UUID;

public interface AuthSessionVerifier {
    boolean isActive(UUID sessionId);
}
