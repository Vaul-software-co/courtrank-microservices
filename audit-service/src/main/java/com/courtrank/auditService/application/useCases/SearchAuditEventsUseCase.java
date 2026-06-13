package com.courtrank.auditService.application.useCases;

import com.courtrank.auditService.application.dto.AuditEventResponse;
import com.courtrank.auditService.application.dto.SearchAuditEventsRequest;
import com.courtrank.auditService.application.dto.SearchAuditEventsResponse;
import com.courtrank.auditService.domain.repository.AuditEventRepository;

public class SearchAuditEventsUseCase {
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final AuditEventRepository auditEventRepository;

    public SearchAuditEventsUseCase(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public SearchAuditEventsResponse execute(SearchAuditEventsRequest request) {
        SearchAuditEventsRequest normalized = normalize(request);

        return new SearchAuditEventsResponse(
                this.auditEventRepository.search(normalized)
                        .stream()
                        .map(AuditEventResponse::from)
                        .toList(),
                normalized.limit(),
                normalized.offset()
        );
    }

    private SearchAuditEventsRequest normalize(SearchAuditEventsRequest request) {
        int requestedLimit = request.limit() <= 0 ? DEFAULT_LIMIT : request.limit();
        int limit = Math.min(requestedLimit, MAX_LIMIT);
        int offset = Math.max(0, request.offset());

        return new SearchAuditEventsRequest(
                blankToNull(request.source()),
                blankToNull(request.type()),
                request.userId(),
                request.actorId(),
                request.targetId(),
                blankToNull(request.traceId()),
                request.occurredFrom(),
                request.occurredTo(),
                limit,
                offset
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
