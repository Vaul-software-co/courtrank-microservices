package com.courtrank.authService.application.ports.security;

import com.courtrank.authService.domain.enums.UserRole;

public record ApiClient(
        String client,
        UserRole type
) {
}
