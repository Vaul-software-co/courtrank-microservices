package com.courtrank.socialService.application.dto;

public record TraceContext(
        String traceId
) {
    public TraceContext {
        if (traceId != null && traceId.isBlank()) {
            traceId = null;
        }
    }

    public static TraceContext fromRequestId(String requestId) {
        return new TraceContext(requestId);
    }

    public static String traceIdOrNull(TraceContext trace) {
        return trace == null ? null : trace.traceId();
    }
}
