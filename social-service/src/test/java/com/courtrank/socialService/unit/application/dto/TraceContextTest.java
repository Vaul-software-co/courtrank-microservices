package com.courtrank.socialService.unit.application.dto;

import com.courtrank.socialService.application.dto.TraceContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TraceContextTest {
    @Test
    void constructor_shouldNormalizeBlankTraceIdToNull() {
        TraceContext trace = new TraceContext("   ");

        assertThat(trace.traceId()).isNull();
    }

    @Test
    void fromRequestId_shouldCreateTraceContext() {
        TraceContext trace = TraceContext.fromRequestId("req-123");

        assertThat(trace.traceId()).isEqualTo("req-123");
    }

    @Test
    void traceIdOrNull_shouldReturnNullWhenTraceIsNull() {
        assertThat(TraceContext.traceIdOrNull(null)).isNull();
    }

    @Test
    void traceIdOrNull_shouldReturnTraceIdWhenTraceExists() {
        assertThat(TraceContext.traceIdOrNull(new TraceContext("req-123"))).isEqualTo("req-123");
    }
}
