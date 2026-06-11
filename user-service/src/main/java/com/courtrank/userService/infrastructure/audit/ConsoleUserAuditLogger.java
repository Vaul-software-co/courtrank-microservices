package com.courtrank.userService.infrastructure.audit;

import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleUserAuditLogger implements UserAuditLogger {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleUserAuditLogger.class);

    @Override
    public void log(UserAuditEvent event) {
        logger.info(
                "user_audit_event type={} actorId={} targetId={} traceId={} metadata={} occurredAt={}",
                event.type(),
                event.actorId(),
                event.targetId(),
                event.traceId(),
                event.metadata(),
                event.occurredAt()
        );
    }
}
