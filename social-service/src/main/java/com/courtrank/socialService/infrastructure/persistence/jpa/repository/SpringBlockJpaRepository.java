package com.courtrank.socialService.infrastructure.persistence.jpa.repository;

import com.courtrank.socialService.infrastructure.persistence.jpa.entity.BlockJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SpringBlockJpaRepository extends JpaRepository<BlockJpaEntity, UUID> {
    Optional<BlockJpaEntity> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    List<BlockJpaEntity> findByBlockerId(UUID blockerId);

    @Query("""
            select count(block) > 0
            from BlockJpaEntity block
            where (block.blockerId = :userA and block.blockedId = :userB)
               or (block.blockerId = :userB and block.blockedId = :userA)
            """)
    boolean existsBetweenUsers(@Param("userA") UUID userA, @Param("userB") UUID userB);

    @Query("""
            select case
                when block.blockerId = :userId then block.blockedId
                else block.blockerId
            end
            from BlockJpaEntity block
            where block.blockerId = :userId or block.blockedId = :userId
            """)
    Set<UUID> findRelatedUserIds(@Param("userId") UUID userId);
}
