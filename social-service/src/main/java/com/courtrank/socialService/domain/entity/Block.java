package com.courtrank.socialService.domain.entity;

import com.courtrank.socialService.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.util.UUID;

public class Block {
    private UUID id;
    private UUID blockerId;
    private UUID blockedId;
    private Instant createdAt;

    private Block(
            UUID id,
            UUID blockerId,
            UUID blockedId,
            Instant createdAt
    ){
        this.id = id;
        this.blockerId = blockerId;
        this.blockedId = blockedId;
        this.createdAt = createdAt;
    }

    private static Block create(
            UUID blockerId,
            UUID blockedId
    ){
        assertDifferentUsers(blockerId, blockedId);

        return new Block(
                UUID.randomUUID(),
                blockerId,
                blockedId,
                Instant.now()
        );
    }

    public static Block blockUser(
            UUID blockerId,
            SocialUser target
    ) {
        if (!target.canBeShown()) {
            throw new DomainValidationException("User cannot be blocked.");
        }

        return create(blockerId, target.getUserId());
    }

    public static Block restore(
            UUID id,
            UUID blockerId,
            UUID blockedId,
            Instant createdAt
    ){
        return new Block(
                id,
                blockerId,
                blockedId,
                createdAt
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getBlockerId() {
        return blockerId;
    }

    public UUID getBlockedId() {
        return blockedId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isOwnedByBlocker(UUID userId) {
        return this.blockerId.equals(userId);
    }

    public boolean blocks(UUID userId) {
        return this.blockedId.equals(userId);
    }

    public boolean isBetween(UUID userA, UUID userB) {
        return (this.blockerId.equals(userA) && this.blockedId.equals(userB))
                || (this.blockerId.equals(userB) && this.blockedId.equals(userA));
    }

    public boolean involves(UUID userId) {
        return this.blockerId.equals(userId) || this.blockedId.equals(userId);
    }

    public boolean blocksInteractionBetween(UUID userA, UUID userB) {
        return isBetween(userA, userB);
    }

    public boolean cancelsFollow(Follow follow) {
        return follow.isBetween(this.blockerId, this.blockedId);
    }

    public boolean canBeRemovedBy(UUID userId) {
        return isOwnedByBlocker(userId);
    }

    public void assertCanBeRemovedBy(UUID userId) {
        if (!canBeRemovedBy(userId)) {
            throw new DomainValidationException("Only the blocker can remove this block.");
        }
    }

    private static void assertDifferentUsers(UUID blockerId, UUID blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new DomainValidationException("You cannot block yourself.");
        }
    }
}
