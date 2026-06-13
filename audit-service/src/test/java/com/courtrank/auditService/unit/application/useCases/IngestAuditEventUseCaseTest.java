package com.courtrank.auditService.unit.application.useCases;

import com.courtrank.auditService.application.dto.AuditEventResponse;
import com.courtrank.auditService.application.dto.IngestAuditEventRequest;
import com.courtrank.auditService.application.useCases.IngestAuditEventUseCase;
import com.courtrank.auditService.domain.entity.AuditEvent;
import com.courtrank.auditService.domain.enums.AuditEventSource;
import com.courtrank.auditService.domain.repository.AuditEventRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IngestAuditEventUseCaseTest {
    @Test
    void execute_shouldSaveAndReturnIngestedAuditEvent() {
        InMemoryAuditEventRepository repository = new InMemoryAuditEventRepository();
        IngestAuditEventUseCase useCase = new IngestAuditEventUseCase(repository);
        UUID eventId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        AuditEventResponse response = useCase.execute(new IngestAuditEventRequest(
                eventId,
                AuditEventSource.AUTH_SERVICE.value(),
                "AUTH_SIGN_IN_SUCCESS",
                actorId,
                targetId,
                "trace-1",
                Map.of("ip", "127.0.0.1"),
                Instant.parse("2026-06-13T10:00:00Z"),
                Instant.parse("2026-06-13T10:00:01Z")
        ));

        assertThat(response.eventId()).isEqualTo(eventId);
        assertThat(response.source()).isEqualTo("auth-service");
        assertThat(response.type()).isEqualTo("AUTH_SIGN_IN_SUCCESS");
        assertThat(response.actorId()).isEqualTo(actorId);
        assertThat(response.targetId()).isEqualTo(targetId);
        assertThat(response.ingestedAt()).isNotNull();
        assertThat(repository.saved).hasSize(1);
        assertThat(repository.saved.get(0).getEventId()).isEqualTo(eventId);
    }

    private static class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> saved = new java.util.ArrayList<>();

        @Override
        public void save(AuditEvent event) {
            this.saved.add(event);
        }

        @Override
        public Optional<AuditEvent> findById(UUID eventId) {
            return Optional.empty();
        }

        @Override
        public List<AuditEvent> search(com.courtrank.auditService.application.dto.SearchAuditEventsRequest request) {
            return List.of();
        }
    }
}
