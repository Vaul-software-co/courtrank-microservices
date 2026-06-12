package com.courtrank.socialService.domain.repository;

import com.courtrank.socialService.domain.entity.Block;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface BlockRepository {
    Block save(Block block);

    Optional<Block> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    List<Block> findByBlockerId(UUID blockerId);

    boolean existsBetweenUsers(UUID userA, UUID userB);

    Set<UUID> findRelatedUserIds(UUID userId);

    void delete(Block block);
}
