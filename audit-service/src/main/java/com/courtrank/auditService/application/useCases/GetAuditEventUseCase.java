package com.courtrank.auditService.application.useCases;

import com.courtrank.auditService.application.dto.AuditEventResponse;
import com.courtrank.auditService.domain.exceptions.AuditEventNotFoundException;
import com.courtrank.auditService.domain.repository.AuditEventRepository;

import java.util.UUID;

public class GetAuditEventUseCase {
    private final AuditEventRepository auditEventRepository;

    public GetAuditEventUseCase(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public AuditEventResponse execute(UUID eventId) {
        return this.auditEventRepository.findById(eventId)
                .map(AuditEventResponse::from)
                .orElseThrow(AuditEventNotFoundException::new);
    }
}
