package com.example.userService.application.ports.audit;

public interface UserAuditLogger {
    void log(UserAuditEvent event);
}
