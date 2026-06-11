package com.courtrank.authService.application.ports.audit;

public interface AuditLogger {
    void log(AuditEvent event);
}
