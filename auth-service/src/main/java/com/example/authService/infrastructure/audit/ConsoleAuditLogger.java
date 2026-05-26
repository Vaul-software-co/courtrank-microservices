package com.example.authService.infrastructure.audit;

import com.example.authService.application.ports.audit.AuditEvent;
import com.example.authService.application.ports.audit.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleAuditLogger implements AuditLogger {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleAuditLogger.class);

    @Override
    public void log(AuditEvent event) {
        logger.info(
                "audit type={} actorId={} targetId={} traceId={} metadata={}",
                event.type(),
                event.actorId(),
                event.targetId(),
                event.traceId(),
                event.metadata()
        );
    }
}
