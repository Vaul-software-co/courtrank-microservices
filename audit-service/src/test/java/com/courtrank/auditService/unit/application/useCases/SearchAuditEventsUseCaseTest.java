package com.courtrank.auditService.unit.application.useCases;

import com.courtrank.auditService.application.dto.SearchAuditEventsRequest;
import com.courtrank.auditService.application.dto.SearchAuditEventsResponse;
import com.courtrank.auditService.application.useCases.SearchAuditEventsUseCase;
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

class SearchAuditEventsUseCaseTest {
    @Test
    void execute_shouldNormalizeFiltersAndPaginationBeforeSearching() {
        CapturingAuditEventRepository repository = new CapturingAuditEventRepository(List.of(auditEvent()));
        SearchAuditEventsUseCase useCase = new SearchAuditEventsUseCase(repository);

        SearchAuditEventsResponse response = useCase.execute(new SearchAuditEventsRequest(
                " user-service ",
                " USER_PROFILE_UPDATED ",
                null,
                null,
                null,
                " ",
                null,
                null,
                500,
                -10
        ));

        assertThat(response.events()).hasSize(1);
        assertThat(response.limit()).isEqualTo(200);
        assertThat(response.offset()).isZero();
        assertThat(repository.lastRequest.source()).isEqualTo("user-service");
        assertThat(repository.lastRequest.type()).isEqualTo("USER_PROFILE_UPDATED");
        assertThat(repository.lastRequest.userId()).isNull();
        assertThat(repository.lastRequest.traceId()).isNull();
        assertThat(repository.lastRequest.limit()).isEqualTo(200);
        assertThat(repository.lastRequest.offset()).isZero();
    }

    @Test
    void execute_shouldUseDefaultLimitWhenLimitIsNotPositive() {
        CapturingAuditEventRepository repository = new CapturingAuditEventRepository(List.of());
        SearchAuditEventsUseCase useCase = new SearchAuditEventsUseCase(repository);

        SearchAuditEventsResponse response = useCase.execute(new SearchAuditEventsRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                25
        ));

        assertThat(response.limit()).isEqualTo(50);
        assertThat(response.offset()).isEqualTo(25);
        assertThat(repository.lastRequest.limit()).isEqualTo(50);
    }

    @Test
    void execute_shouldKeepUserIdFilterForActorOrTargetQueries() {
        CapturingAuditEventRepository repository = new CapturingAuditEventRepository(List.of());
        SearchAuditEventsUseCase useCase = new SearchAuditEventsUseCase(repository);
        UUID userId = UUID.randomUUID();

        useCase.execute(new SearchAuditEventsRequest(
                null,
                null,
                userId,
                null,
                null,
                null,
                null,
                null,
                50,
                0
        ));

        assertThat(repository.lastRequest.userId()).isEqualTo(userId);
    }

    private static AuditEvent auditEvent() {
        return AuditEvent.restore(
                UUID.randomUUID(),
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

    private static class CapturingAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events;
        private SearchAuditEventsRequest lastRequest;

        private CapturingAuditEventRepository(List<AuditEvent> events) {
            this.events = events;
        }

        @Override
        public void save(AuditEvent event) {
        }

        @Override
        public Optional<AuditEvent> findById(UUID eventId) {
            return Optional.empty();
        }

        @Override
        public List<AuditEvent> search(SearchAuditEventsRequest request) {
            this.lastRequest = request;
            return this.events;
        }
    }
}
