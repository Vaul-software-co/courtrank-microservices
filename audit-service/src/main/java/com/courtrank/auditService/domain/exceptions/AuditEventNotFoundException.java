package com.courtrank.auditService.domain.exceptions;

public class AuditEventNotFoundException extends RuntimeException {
    public AuditEventNotFoundException() {
        super("Audit event not found");
    }
}
