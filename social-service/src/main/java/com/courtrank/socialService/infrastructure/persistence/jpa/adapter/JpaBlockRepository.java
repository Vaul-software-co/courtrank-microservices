package com.courtrank.socialService.infrastructure.persistence.jpa.adapter;

import com.courtrank.socialService.domain.entity.Block;
import com.courtrank.socialService.domain.repository.BlockRepository;
import com.courtrank.socialService.infrastructure.persistence.jpa.entity.BlockJpaEntity;
import com.courtrank.socialService.infrastructure.persistence.jpa.repository.SpringBlockJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class JpaBlockRepository implements BlockRepository {
    private final SpringBlockJpaRepository repository;

    public JpaBlockRepository(SpringBlockJpaRepository repository) {
        this.repository = repository;
    }

    @Override public Block save(Block block) { return this.repository.save(BlockJpaEntity.fromDomain(block)).toDomain(); }
    @Override public Optional<Block> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId) { return this.repository.findByBlockerIdAndBlockedId(blockerId, blockedId).map(BlockJpaEntity::toDomain); }
    @Override public List<Block> findByBlockerId(UUID blockerId) { return this.repository.findByBlockerId(blockerId).stream().map(BlockJpaEntity::toDomain).toList(); }
    @Override public boolean existsBetweenUsers(UUID userA, UUID userB) { return this.repository.existsBetweenUsers(userA, userB); }
    @Override public Set<UUID> findRelatedUserIds(UUID userId) { return this.repository.findRelatedUserIds(userId); }
    @Override public void delete(Block block) { this.repository.deleteById(block.getId()); }
}
