package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.application.ports.audit.SocialAuditEvent;
import com.courtrank.socialService.application.ports.audit.SocialAuditEventType;
import com.courtrank.socialService.application.ports.audit.SocialAuditLogger;
import com.courtrank.socialService.domain.entity.SocialCounter;
import com.courtrank.socialService.domain.repository.SocialCounterRepository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

abstract class SocialUseCaseSupport {
    protected SocialCounter findOrCreateCounter(SocialCounterRepository repository, UUID userId) {
        return repository.findByUserId(userId).orElseGet(() -> SocialCounter.create(userId));
    }

    protected void log(
            SocialAuditLogger auditLogger,
            SocialAuditEventType type,
            UUID actorId,
            UUID targetId,
            TraceContext trace,
            Map<String, Object> metadata
    ) {
        auditLogger.log(new SocialAuditEvent(
                type,
                actorId,
                targetId,
                TraceContext.traceIdOrNull(trace),
                metadata,
                Instant.now()
        ));
    }
}
