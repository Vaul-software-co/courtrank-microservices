package com.courtrank.userService.application.ports.security;

import java.util.UUID;

public interface TokenService {
    boolean verifyAccess(String token);
    UUID getTokenId(String token);
    UUID getSessionId(String token);
}
