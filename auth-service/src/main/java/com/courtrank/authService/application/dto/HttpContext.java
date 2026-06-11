package com.courtrank.authService.application.dto;

import com.courtrank.authService.domain.enums.UserRole;

public record HttpContext(
        String client,
        String ip,
        String userAgent,
        UserRole type,
        String traceId
) {
    public HttpContext(String client, String ip, String userAgent, UserRole type) {
        this(client, ip, userAgent, type, null);
    }
}
