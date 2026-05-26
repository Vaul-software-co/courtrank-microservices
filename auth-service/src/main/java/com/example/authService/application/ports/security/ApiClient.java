package com.example.authService.application.ports.security;

import com.example.authService.domain.enums.UserRole;

public record ApiClient(
        String client,
        UserRole type
) {
}
