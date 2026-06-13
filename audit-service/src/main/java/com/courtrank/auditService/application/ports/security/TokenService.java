package com.courtrank.auditService.application.ports.security;

import java.util.UUID;

public interface TokenService {
    boolean verifyAccess(String token);
    UUID getTokenId(String token);
    String getRole(String token);
}
