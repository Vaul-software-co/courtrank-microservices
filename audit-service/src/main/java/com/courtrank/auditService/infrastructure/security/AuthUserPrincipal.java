package com.courtrank.auditService.infrastructure.security;

import java.util.UUID;

public record AuthUserPrincipal(UUID userId, String role) {
}
