package com.courtrank.auditService.infrastructure.persistence.jpa.adapter;

import com.courtrank.auditService.application.dto.SearchAuditEventsRequest;
import com.courtrank.auditService.domain.entity.AuditEvent;
import com.courtrank.auditService.domain.repository.AuditEventRepository;
import com.courtrank.auditService.infrastructure.persistence.jpa.OffsetBasedPageRequest;
import com.courtrank.auditService.infrastructure.persistence.jpa.entity.AuditEventJpaEntity;
import com.courtrank.auditService.infrastructure.persistence.jpa.repository.SpringAuditEventJpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaAuditEventRepository implements AuditEventRepository {
    private final SpringAuditEventJpaRepository repository;

    public JpaAuditEventRepository(SpringAuditEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(AuditEvent event) {
        this.repository.save(AuditEventJpaEntity.fromDomain(event));
    }

    @Override
    public Optional<AuditEvent> findById(UUID eventId) {
        return this.repository.findById(eventId)
                .map(AuditEventJpaEntity::toDomain);
    }

    @Override
    public List<AuditEvent> search(SearchAuditEventsRequest request) {
        Pageable pageable = new OffsetBasedPageRequest(request.limit(), request.offset());
        return this.repository.search(
                        request.source(),
                        request.type(),
                        request.userId(),
                        request.actorId(),
                        request.targetId(),
                        request.traceId(),
                        request.occurredFrom(),
                        request.occurredTo(),
                        pageable
                )
                .stream()
                .map(AuditEventJpaEntity::toDomain)
                .toList();
    }
}
