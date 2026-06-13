package com.courtrank.authService.infrastructure.persistence.jpa.repository;

import com.courtrank.authService.domain.enums.SessionStatus;
import com.courtrank.authService.infrastructure.persistence.jpa.entity.SessionJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringSessionJpaRepository extends JpaRepository<SessionJpaEntity, UUID> {
    Optional<SessionJpaEntity> findByRefreshTokenHash(String refreshTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session
            from SessionJpaEntity session
            where session.refreshTokenHash = :refreshTokenHash
            """)
    Optional<SessionJpaEntity> findByRefreshTokenHashForUpdate(
            @Param("refreshTokenHash") String refreshTokenHash
    );

    List<SessionJpaEntity> findByUserIdAndStatus(UUID userId, SessionStatus status);

    @Modifying
    @Query("""
            update SessionJpaEntity session
            set session.replacedBy = :newSessionId,
                session.status = com.courtrank.authService.domain.enums.SessionStatus.REPLACED,
                session.revokedAt = :now
            where session.refreshTokenHash = :oldTokenHash
              and session.status = com.courtrank.authService.domain.enums.SessionStatus.ACTIVE
            """)
    int markActiveSessionAsReplaced(
            @Param("oldTokenHash") String oldTokenHash,
            @Param("newSessionId") UUID newSessionId,
            @Param("now") Instant now
    );

    @Modifying
    @Query("""
            update SessionJpaEntity session
            set session.status = com.courtrank.authService.domain.enums.SessionStatus.REVOKED,
                session.revokedAt = :now
            where session.userId = :userId
              and session.status = com.courtrank.authService.domain.enums.SessionStatus.ACTIVE
            """)
    void revokeActiveByUserId(
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );

    @Modifying
    @Query("""
            delete from SessionJpaEntity session
            where session.expiresAt < :cutoff
               or (
                    session.status <> com.courtrank.authService.domain.enums.SessionStatus.ACTIVE
                    and session.revokedAt is not null
                    and session.revokedAt < :cutoff
               )
            """)
    int deleteInactiveBefore(@Param("cutoff") Instant cutoff);
}
