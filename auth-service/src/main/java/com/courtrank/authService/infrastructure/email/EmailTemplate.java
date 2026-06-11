package com.courtrank.authService.infrastructure.email;

public record EmailTemplate(
        String subject,
        String html
) {
}
