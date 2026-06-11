package com.courtrank.authService.infrastructure.persistence.jpa.repository;

import com.courtrank.authService.domain.enums.VerificationTokenType;
import com.courtrank.authService.infrastructure.persistence.jpa.entity.VerificationTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SpringVerificationTokenJpaRepository extends JpaRepository<VerificationTokenJpaEntity, UUID> {
    Optional<VerificationTokenJpaEntity> findFirstByUserIdAndTokenHashAndTypeAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID userId,
            String tokenHash,
            VerificationTokenType type,
            Instant now
    );

    Optional<VerificationTokenJpaEntity> findFirstByUserIdAndTypeAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID userId,
            VerificationTokenType type,
            Instant now
    );

    @Modifying
    @Query("""
            update VerificationTokenJpaEntity token
            set token.usedAt = :now
            where token.userId = :userId
              and token.type = :type
              and token.usedAt is null
            """)
    void markUnusedAsUsed(
            @Param("userId") UUID userId,
            @Param("type") VerificationTokenType type,
            @Param("now") Instant now
    );
}
