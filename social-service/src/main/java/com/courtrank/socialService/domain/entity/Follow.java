package com.courtrank.socialService.domain.entity;

import com.courtrank.socialService.domain.enums.FollowStatus;
import com.courtrank.socialService.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.util.UUID;

public class Follow {
    private UUID id;
    private UUID followerId;
    private UUID followingId;
    private FollowStatus followStatus;
    private Instant createdAt;
    private Instant acceptedAt;
    private Instant updatedAt;

    private Follow(
            UUID id,
            UUID followerId,
            UUID followingId,
            FollowStatus followStatus,
            Instant createdAt,
            Instant acceptedAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.followerId = followerId;
        this.followingId = followingId;
        this.followStatus = followStatus;
        this.createdAt = createdAt;
        this.acceptedAt = acceptedAt;
        this.updatedAt = updatedAt;
    }

    private static Follow create(
            UUID followerId,
            UUID followingId,
            FollowStatus followStatus
    ) {
        assertDifferentUsers(followerId, followingId);

        Instant now = Instant.now();
        Instant acceptedAt = followStatus == FollowStatus.ACCEPTED ? now : null;

        return new Follow(
                UUID.randomUUID(),
                followerId,
                followingId,
                followStatus,
                now,
                acceptedAt,
                now
        );
    }

    public static Follow startFollowing(
            UUID followerId,
            SocialUser target
    ) {
        if (!target.canBeShown()) {
            throw new DomainValidationException("User cannot be followed.");
        }

        return create(
                followerId,
                target.getUserId(),
                target.isPrivate() ? FollowStatus.PENDING : FollowStatus.ACCEPTED
        );
    }

    public static Follow restore(
            UUID id,
            UUID followerId,
            UUID followingId,
            FollowStatus followStatus,
            Instant createdAt,
            Instant acceptedAt,
            Instant updatedAt
    ){
        return new Follow(
                id,
                followerId,
                followingId,
                followStatus,
                createdAt,
                acceptedAt,
                updatedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getFollowerId() {
        return followerId;
    }

    public UUID getFollowingId() {
        return followingId;
    }

    public FollowStatus getFollowStatus() {
        return followStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isPending() {
        return this.followStatus == FollowStatus.PENDING;
    }

    public boolean isAccepted() {
        return this.followStatus == FollowStatus.ACCEPTED;
    }

    public boolean isBetween(UUID userA, UUID userB) {
        return (this.followerId.equals(userA) && this.followingId.equals(userB))
                || (this.followerId.equals(userB) && this.followingId.equals(userA));
    }

    public boolean isOwnedByFollower(UUID userId) {
        return this.followerId.equals(userId);
    }

    public boolean isOwnedByFollowing(UUID userId) {
        return this.followingId.equals(userId);
    }

    public boolean canBeCanceledBy(UUID userId) {
        return isPending() && isOwnedByFollower(userId);
    }

    public boolean canBeUnfollowedBy(UUID userId) {
        return isAccepted() && isOwnedByFollower(userId);
    }

    public boolean canBeRemovedBy(UUID userId) {
        return isAccepted() && isOwnedByFollowing(userId);
    }

    public void accept(UUID ownerId) {
        assertManagedByFollowing(ownerId);

        if (isAccepted()) {
            return;
        }

        Instant now = Instant.now();
        this.followStatus = FollowStatus.ACCEPTED;
        this.acceptedAt = now;
        this.updatedAt = now;
    }

    public void assertCanBeCanceledBy(UUID userId) {
        if (!canBeCanceledBy(userId)) {
            throw new DomainValidationException("Only the requester can cancel this follow request.");
        }
    }

    public void assertCanBeUnfollowedBy(UUID userId) {
        if (!canBeUnfollowedBy(userId)) {
            throw new DomainValidationException("Only the follower can unfollow this user.");
        }
    }

    public void assertCanBeRemovedBy(UUID userId) {
        if (!canBeRemovedBy(userId)) {
            throw new DomainValidationException("Only the followed user can remove this follower.");
        }
    }

    public void assertManagedByFollowing(UUID ownerId) {
        if (!isOwnedByFollowing(ownerId)) {
            throw new DomainValidationException("Only the followed user can manage this follow request.");
        }
    }

    private static void assertDifferentUsers(UUID followerId, UUID followingId) {
        if (followerId.equals(followingId)) {
            throw new DomainValidationException("You cannot follow yourself.");
        }
    }
}
