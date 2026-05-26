package com.example.authService.infrastructure.email;

public record EmailTemplate(
        String subject,
        String html
) {
}
