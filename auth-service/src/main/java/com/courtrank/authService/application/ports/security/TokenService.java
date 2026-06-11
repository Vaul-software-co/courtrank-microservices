package com.courtrank.authService.application.ports.security;

import com.courtrank.authService.domain.enums.TokenType;
import com.courtrank.authService.domain.enums.UserRole;

import java.util.UUID;

public interface TokenService {
    String generateToken(UUID id, TokenType type);
    String generateToken(UUID id, TokenType type, UserRole userType);
    String generateAccessToken(UUID id, UUID sessionId, UserRole userType);
    String generatePasswordResetToken(UUID id, UUID tokenId);
    boolean verifyAccess(String token);
    boolean verifyRefresh(String token);
    boolean verifyPasswordReset(String token);
    UUID getTokenId(String token);
    UUID getSessionId(String token);
    UUID getTokenJti(String token);
}
