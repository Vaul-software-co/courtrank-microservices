package com.courtrank.userService.application.ports.audit;

public interface UserAuditLogger {
    void log(UserAuditEvent event);
}
