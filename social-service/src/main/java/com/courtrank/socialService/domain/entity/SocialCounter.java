package com.courtrank.socialService.domain.entity;

import com.courtrank.socialService.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.util.UUID;

public class SocialCounter {
    private UUID userId;
    private int followersCount;
    private int followingCount;
    private int pendingRequestsCount;
    private int blockedCount;
    private Instant createdAt;
    private Instant updatedAt;

    private SocialCounter (
            UUID userId,
            int followersCount,
            int followingCount,
            int pendingRequestsCount,
            int blockedCount,
            Instant createdAt,
            Instant updatedAt
    ){
        assertNonNegative(followersCount, "followersCount");
        assertNonNegative(followingCount, "followingCount");
        assertNonNegative(pendingRequestsCount, "pendingRequestsCount");
        assertNonNegative(blockedCount, "blockedCount");

        this.userId = userId;
        this.followersCount = followersCount;
        this.followingCount = followingCount;
        this.pendingRequestsCount = pendingRequestsCount;
        this.blockedCount = blockedCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SocialCounter create(UUID userId){
        Instant now = Instant.now();
        return new SocialCounter(
                userId,
                0,
                0,
                0,
                0,
                now,
                now
        );
    }

    public static SocialCounter restore(
            UUID userId,
            int followersCount,
            int followingCount,
            int pendingRequestsCount,
            int blockedCount,
            Instant createdAt,
            Instant updatedAt
    ){
        return new SocialCounter(
                userId,
                followersCount,
                followingCount,
                pendingRequestsCount,
                blockedCount,
                createdAt,
                updatedAt
        );
    }

    public UUID getUserId() {
        return userId;
    }

    public int getFollowersCount() {
        return followersCount;
    }

    public int getFollowingCount() {
        return followingCount;
    }

    public int getPendingRequestsCount() {
        return pendingRequestsCount;
    }

    public int getBlockedCount() {
        return blockedCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void incrementFollowers() {
        this.followersCount++;
        touch();
    }

    public void decrementFollowers() {
        this.followersCount = decrement(this.followersCount, "followersCount");
        touch();
    }

    public void incrementFollowing() {
        this.followingCount++;
        touch();
    }

    public void decrementFollowing() {
        this.followingCount = decrement(this.followingCount, "followingCount");
        touch();
    }

    public void incrementPendingRequests() {
        this.pendingRequestsCount++;
        touch();
    }

    public void decrementPendingRequests() {
        this.pendingRequestsCount = decrement(this.pendingRequestsCount, "pendingRequestsCount");
        touch();
    }

    public void incrementBlocked() {
        this.blockedCount++;
        touch();
    }

    public void decrementBlocked() {
        this.blockedCount = decrement(this.blockedCount, "blockedCount");
        touch();
    }

    public void applyAcceptedFollowAsFollower() {
        incrementFollowing();
    }

    public void removeAcceptedFollowAsFollower() {
        decrementFollowing();
    }

    public void applyAcceptedFollowAsFollowing() {
        incrementFollowers();
    }

    public void removeAcceptedFollowAsFollowing() {
        decrementFollowers();
    }

    public void applyPendingRequestAsFollowing() {
        incrementPendingRequests();
    }

    public void removePendingRequestAsFollowing() {
        decrementPendingRequests();
    }

    public void acceptPendingRequestAsFollower() {
        incrementFollowing();
    }

    public void acceptPendingRequestAsFollowing() {
        decrementPendingRequests();
        incrementFollowers();
    }

    public void applyBlockAsBlocker() {
        incrementBlocked();
    }

    public void removeBlockAsBlocker() {
        decrementBlocked();
    }

    private static void assertNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new DomainValidationException(fieldName + " cannot be negative.");
        }
    }

    private static int decrement(int value, String fieldName) {
        if (value == 0) {
            throw new DomainValidationException(fieldName + " cannot be negative.");
        }

        return value - 1;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
