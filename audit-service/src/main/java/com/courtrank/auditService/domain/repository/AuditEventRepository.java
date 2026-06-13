package com.courtrank.auditService.domain.repository;

import com.courtrank.auditService.application.dto.SearchAuditEventsRequest;
import com.courtrank.auditService.domain.entity.AuditEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditEventRepository {
    void save(AuditEvent event);
    Optional<AuditEvent> findById(UUID eventId);
    List<AuditEvent> search(SearchAuditEventsRequest request);
}
