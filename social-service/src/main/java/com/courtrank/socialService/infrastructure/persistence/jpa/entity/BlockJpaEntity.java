package com.courtrank.socialService.infrastructure.persistence.jpa.entity;

import com.courtrank.socialService.domain.entity.Block;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blocks")
public class BlockJpaEntity {
    @Id
    private UUID id;

    @Column(name = "blocker_id", nullable = false)
    private UUID blockerId;

    @Column(name = "blocked_id", nullable = false)
    private UUID blockedId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected BlockJpaEntity() {
    }

    public BlockJpaEntity(UUID id, UUID blockerId, UUID blockedId, Instant createdAt) {
        this.id = id;
        this.blockerId = blockerId;
        this.blockedId = blockedId;
        this.createdAt = createdAt;
    }

    public static BlockJpaEntity fromDomain(Block block) {
        return new BlockJpaEntity(block.getId(), block.getBlockerId(), block.getBlockedId(), block.getCreatedAt());
    }

    public Block toDomain() {
        return Block.restore(this.id, this.blockerId, this.blockedId, this.createdAt);
    }
}
