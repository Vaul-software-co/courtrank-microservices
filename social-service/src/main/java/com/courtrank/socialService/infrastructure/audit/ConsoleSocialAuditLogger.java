package com.courtrank.socialService.infrastructure.audit;

import com.courtrank.socialService.application.ports.audit.SocialAuditEvent;
import com.courtrank.socialService.application.ports.audit.SocialAuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleSocialAuditLogger implements SocialAuditLogger {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleSocialAuditLogger.class);

    @Override
    public void log(SocialAuditEvent event) {
        logger.info(
                "social_audit_event type={} actorId={} targetId={} traceId={} metadata={} occurredAt={}",
                event.type(),
                event.actorId(),
                event.targetId(),
                event.traceId(),
                event.metadata(),
                event.occurredAt()
        );
    }
}
