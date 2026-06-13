package com.courtrank.auditService.application.dto;

import java.util.List;

public record SearchAuditEventsResponse(
        List<AuditEventResponse> events,
        int limit,
        int offset
) {
}
