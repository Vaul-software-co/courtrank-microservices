package com.courtrank.auditService.unit.application.useCases;

import com.courtrank.auditService.application.dto.AuditEventResponse;
import com.courtrank.auditService.application.dto.SearchAuditEventsRequest;
import com.courtrank.auditService.application.useCases.GetAuditEventUseCase;
import com.courtrank.auditService.domain.entity.AuditEvent;
import com.courtrank.auditService.domain.enums.AuditEventSource;
import com.courtrank.auditService.domain.exceptions.AuditEventNotFoundException;
import com.courtrank.auditService.domain.repository.AuditEventRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetAuditEventUseCaseTest {
    @Test
    void execute_shouldReturnAuditEventById() {
        UUID eventId = UUID.randomUUID();
        AuditEvent event = auditEvent(eventId);
        GetAuditEventUseCase useCase = new GetAuditEventUseCase(new SingleAuditEventRepository(event));

        AuditEventResponse response = useCase.execute(eventId);

        assertThat(response.eventId()).isEqualTo(eventId);
        assertThat(response.source()).isEqualTo("user-service");
        assertThat(response.type()).isEqualTo("USER_PROFILE_UPDATED");
    }

    @Test
    void execute_shouldThrowWhenAuditEventDoesNotExist() {
        GetAuditEventUseCase useCase = new GetAuditEventUseCase(new SingleAuditEventRepository(null));

        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID()))
                .isInstanceOf(AuditEventNotFoundException.class);
    }

    private static AuditEvent auditEvent(UUID eventId) {
        return AuditEvent.restore(
                eventId,
                AuditEventSource.USER_SERVICE.value(),
                "USER_PROFILE_UPDATED",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "trace-1",
                Map.of(),
                Instant.parse("2026-06-13T10:00:00Z"),
                Instant.parse("2026-06-13T10:00:01Z"),
                Instant.parse("2026-06-13T10:00:02Z")
        );
    }

    private static class SingleAuditEventRepository implements AuditEventRepository {
        private final AuditEvent event;

        private SingleAuditEventRepository(AuditEvent event) {
            this.event = event;
        }

        @Override
        public void save(AuditEvent event) {
        }

        @Override
        public Optional<AuditEvent> findById(UUID eventId) {
            return this.event != null && this.event.getEventId().equals(eventId)
                    ? Optional.of(this.event)
                    : Optional.empty();
        }

        @Override
        public List<AuditEvent> search(SearchAuditEventsRequest request) {
            return List.of();
        }
    }
}
