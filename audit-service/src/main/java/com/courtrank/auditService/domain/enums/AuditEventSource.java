package com.courtrank.auditService.domain.enums;

public enum AuditEventSource {
    AUTH_SERVICE("auth-service"),
    USER_SERVICE("user-service"),
    SOCIAL_SERVICE("social-service");

    private final String value;

    AuditEventSource(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }
}
