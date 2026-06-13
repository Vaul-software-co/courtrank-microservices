package com.courtrank.auditService.infrastructure.persistence.jpa.repository;

import com.courtrank.auditService.infrastructure.persistence.jpa.entity.AuditEventJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SpringAuditEventJpaRepository extends JpaRepository<AuditEventJpaEntity, UUID> {
    @Query("""
            select event
            from AuditEventJpaEntity event
            where (:source is null or event.source = :source)
              and (:type is null or event.type = :type)
              and (:userId is null or event.actorId = :userId or event.targetId = :userId)
              and (:actorId is null or event.actorId = :actorId)
              and (:targetId is null or event.targetId = :targetId)
              and (:traceId is null or event.traceId = :traceId)
              and (:occurredFrom is null or event.occurredAt >= :occurredFrom)
              and (:occurredTo is null or event.occurredAt <= :occurredTo)
            order by event.occurredAt desc, event.ingestedAt desc
            """)
    List<AuditEventJpaEntity> search(
            @Param("source") String source,
            @Param("type") String type,
            @Param("userId") UUID userId,
            @Param("actorId") UUID actorId,
            @Param("targetId") UUID targetId,
            @Param("traceId") String traceId,
            @Param("occurredFrom") Instant occurredFrom,
            @Param("occurredTo") Instant occurredTo,
            Pageable pageable
    );
}
