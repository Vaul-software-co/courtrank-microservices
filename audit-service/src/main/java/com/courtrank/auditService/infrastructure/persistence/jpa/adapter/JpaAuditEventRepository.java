package com.courtrank.auditService.infrastructure.persistence.jpa.adapter;

import com.courtrank.auditService.application.dto.SearchAuditEventsRequest;
import com.courtrank.auditService.domain.entity.AuditEvent;
import com.courtrank.auditService.domain.repository.AuditEventRepository;
import com.courtrank.auditService.infrastructure.persistence.jpa.OffsetBasedPageRequest;
import com.courtrank.auditService.infrastructure.persistence.jpa.entity.AuditEventJpaEntity;
import com.courtrank.auditService.infrastructure.persistence.jpa.repository.SpringAuditEventJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaAuditEventRepository implements AuditEventRepository {
    private final SpringAuditEventJpaRepository repository;
    private final EntityManager entityManager;

    public JpaAuditEventRepository(SpringAuditEventJpaRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
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
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditEventJpaEntity> query = cb.createQuery(AuditEventJpaEntity.class);
        Root<AuditEventJpaEntity> event = query.from(AuditEventJpaEntity.class);
        List<Predicate> predicates = new ArrayList<>();

        if (request.source() != null) {
            predicates.add(cb.equal(event.get("source"), request.source()));
        }
        if (request.type() != null) {
            predicates.add(cb.equal(event.get("type"), request.type()));
        }
        if (request.userId() != null) {
            predicates.add(cb.or(
                    cb.equal(event.get("actorId"), request.userId()),
                    cb.equal(event.get("targetId"), request.userId())
            ));
        }
        if (request.actorId() != null) {
            predicates.add(cb.equal(event.get("actorId"), request.actorId()));
        }
        if (request.targetId() != null) {
            predicates.add(cb.equal(event.get("targetId"), request.targetId()));
        }
        if (request.traceId() != null) {
            predicates.add(cb.equal(event.get("traceId"), request.traceId()));
        }
        if (request.occurredFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(event.get("occurredAt"), request.occurredFrom()));
        }
        if (request.occurredTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(event.get("occurredAt"), request.occurredTo()));
        }

        query.select(event)
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(cb.desc(event.get("occurredAt")), cb.desc(event.get("ingestedAt")));

        return this.entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList()
                .stream()
                .map(AuditEventJpaEntity::toDomain)
                .toList();
    }
}
