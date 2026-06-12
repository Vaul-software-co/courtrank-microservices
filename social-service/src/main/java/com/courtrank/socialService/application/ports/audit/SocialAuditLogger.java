package com.courtrank.socialService.application.ports.audit;

public interface SocialAuditLogger {
    void log(SocialAuditEvent event);
}
