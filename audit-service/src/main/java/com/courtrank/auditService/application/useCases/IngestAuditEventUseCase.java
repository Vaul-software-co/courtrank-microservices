package com.courtrank.auditService.application.useCases;

import com.courtrank.auditService.application.dto.AuditEventResponse;
import com.courtrank.auditService.application.dto.IngestAuditEventRequest;
import com.courtrank.auditService.domain.entity.AuditEvent;
import com.courtrank.auditService.domain.repository.AuditEventRepository;

public class IngestAuditEventUseCase {
    private final AuditEventRepository auditEventRepository;

    public IngestAuditEventUseCase(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public AuditEventResponse execute(IngestAuditEventRequest request) {
        AuditEvent event = AuditEvent.ingest(
                request.eventId(),
                request.source(),
                request.type(),
                request.actorId(),
                request.targetId(),
                request.traceId(),
                request.metadata(),
                request.occurredAt(),
                request.publishedAt()
        );

        this.auditEventRepository.save(event);

        return AuditEventResponse.from(event);
    }
}
