package com.courtrank.auditService.infrastructure.controllers;

import com.courtrank.auditService.application.dto.AuditEventResponse;
import com.courtrank.auditService.application.dto.SearchAuditEventsRequest;
import com.courtrank.auditService.application.dto.SearchAuditEventsResponse;
import com.courtrank.auditService.application.useCases.GetAuditEventUseCase;
import com.courtrank.auditService.application.useCases.SearchAuditEventsUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/audit/events")
public class AuditEventController {
    private final SearchAuditEventsUseCase searchAuditEventsUseCase;
    private final GetAuditEventUseCase getAuditEventUseCase;

    public AuditEventController(
            SearchAuditEventsUseCase searchAuditEventsUseCase,
            GetAuditEventUseCase getAuditEventUseCase
    ) {
        this.searchAuditEventsUseCase = searchAuditEventsUseCase;
        this.getAuditEventUseCase = getAuditEventUseCase;
    }

    @GetMapping
    public SearchAuditEventsResponse search(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) UUID targetId,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredTo,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return this.searchAuditEventsUseCase.execute(new SearchAuditEventsRequest(
                source,
                type,
                userId,
                actorId,
                targetId,
                traceId,
                occurredFrom,
                occurredTo,
                limit,
                offset
        ));
    }

    @GetMapping("/{eventId}")
    public AuditEventResponse getById(@PathVariable UUID eventId) {
        return this.getAuditEventUseCase.execute(eventId);
    }
}
