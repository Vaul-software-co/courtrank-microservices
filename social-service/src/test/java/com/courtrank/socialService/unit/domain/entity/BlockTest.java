package com.courtrank.socialService.unit.domain.entity;

import com.courtrank.socialService.domain.entity.Block;
import com.courtrank.socialService.domain.entity.Follow;
import com.courtrank.socialService.domain.entity.SocialUser;
import com.courtrank.socialService.domain.enums.FollowStatus;
import com.courtrank.socialService.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BlockTest {
    @Test
    void blockUser_shouldCreateBlockAgainstVisibleTarget() {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        SocialUser target = visibleUser(blockedId);

        Block block = Block.blockUser(blockerId, target);

        assertThat(block.getId()).isNotNull();
        assertThat(block.getBlockerId()).isEqualTo(blockerId);
        assertThat(block.getBlockedId()).isEqualTo(blockedId);
        assertThat(block.getCreatedAt()).isNotNull();
        assertThat(block.isOwnedByBlocker(blockerId)).isTrue();
        assertThat(block.blocks(blockedId)).isTrue();
    }

    @Test
    void restore_shouldRoundTripAllStoredFields() {
        UUID id = UUID.randomUUID();
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        Block block = Block.restore(id, blockerId, blockedId, createdAt);

        assertThat(block.getId()).isEqualTo(id);
        assertThat(block.getBlockerId()).isEqualTo(blockerId);
        assertThat(block.getBlockedId()).isEqualTo(blockedId);
        assertThat(block.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void blockUser_shouldRejectSelfBlock() {
        UUID userId = UUID.randomUUID();
        SocialUser target = visibleUser(userId);

        assertThatThrownBy(() -> Block.blockUser(userId, target))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void blockUser_shouldRejectInactiveOrDeletedTarget() {
        UUID blockerId = UUID.randomUUID();
        SocialUser target = visibleUser(UUID.randomUUID());
        target.markDeleted(
                Instant.parse("2026-01-02T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z")
        );

        assertThatThrownBy(() -> Block.blockUser(blockerId, target))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void helpers_shouldIdentifyBlockedRelationshipInBothDirections() {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Block block = Block.blockUser(blockerId, visibleUser(blockedId));

        assertThat(block.involves(blockerId)).isTrue();
        assertThat(block.involves(blockedId)).isTrue();
        assertThat(block.involves(otherId)).isFalse();
        assertThat(block.isBetween(blockerId, blockedId)).isTrue();
        assertThat(block.isBetween(blockedId, blockerId)).isTrue();
        assertThat(block.blocksInteractionBetween(blockedId, blockerId)).isTrue();
        assertThat(block.blocksInteractionBetween(blockerId, otherId)).isFalse();
    }

    @Test
    void cancelsFollow_shouldMatchFollowsBetweenBothUsers() {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Block block = Block.blockUser(blockerId, visibleUser(blockedId));
        Follow directFollow = Follow.restore(
                UUID.randomUUID(),
                blockerId,
                blockedId,
                FollowStatus.ACCEPTED,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );
        Follow reverseFollow = Follow.restore(
                UUID.randomUUID(),
                blockedId,
                blockerId,
                FollowStatus.PENDING,
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        Follow unrelatedFollow = Follow.restore(
                UUID.randomUUID(),
                blockerId,
                otherId,
                FollowStatus.ACCEPTED,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );

        assertThat(block.cancelsFollow(directFollow)).isTrue();
        assertThat(block.cancelsFollow(reverseFollow)).isTrue();
        assertThat(block.cancelsFollow(unrelatedFollow)).isFalse();
    }

    @Test
    void block_shouldOnlyBeRemovableByBlocker() {
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        Block block = Block.blockUser(blockerId, visibleUser(blockedId));

        assertThat(block.canBeRemovedBy(blockerId)).isTrue();
        assertThat(block.canBeRemovedBy(blockedId)).isFalse();
        assertThatThrownBy(() -> block.assertCanBeRemovedBy(blockedId))
                .isInstanceOf(DomainValidationException.class);
    }

    private SocialUser visibleUser(UUID userId) {
        return SocialUser.create(
                userId,
                "Target",
                "target",
                null,
                false,
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
