package com.courtrank.socialService.domain.entity;

import java.time.Instant;
import java.util.UUID;

public class SocialUser {
    private UUID userId;
    private String name;
    private String username;
    private String avatarUrl;
    private boolean isPrivate;
    private boolean isActive;
    private Instant deletedAt;
    private Instant sourceUpdatedAt;
    private Instant syncedAt;
    private Instant createdAt;
    private Instant updatedAt;

    private SocialUser(
            UUID userId,
            String name,
            String username,
            String avatarUrl,
            boolean isPrivate,
            boolean isActive,
            Instant deletedAt,
            Instant sourceUpdatedAt,
            Instant syncedAt,
            Instant createdAt,
            Instant updatedAt
    ){
        this.userId = userId;
        this.name = name;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.isPrivate = isPrivate;
        this.isActive = isActive;
        this.deletedAt = deletedAt;
        this.sourceUpdatedAt = sourceUpdatedAt;
        this.syncedAt = syncedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SocialUser create(
        UUID userId,
        String name,
        String username,
        String avatarUrl,
        boolean isPrivate,
        boolean isActive,
        Instant sourceUpdatedAt
    ){
        Instant now = Instant.now();
        return new SocialUser(
                userId,
                name,
                username,
                avatarUrl,
                isPrivate,
                isActive,
                null,
                sourceUpdatedAt,
                now,
                now,
                now
        );
    }

    public static SocialUser create(
            UUID userId,
            String name,
            String username,
            String avatarUrl
    ){
        return create(userId, name, username, avatarUrl, false, true, Instant.now());
    }

    public static SocialUser restore(
            UUID userId,
            String name,
            String username,
            String avatarUrl,
            boolean isPrivate,
            boolean isActive,
            Instant deletedAt,
            Instant sourceUpdatedAt,
            Instant syncedAt,
            Instant createdAt,
            Instant updatedAt
    ){
        return new SocialUser(
                userId,
                name,
                username,
                avatarUrl,
                isPrivate,
                isActive,
                deletedAt,
                sourceUpdatedAt,
                syncedAt,
                createdAt,
                updatedAt
        );
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public boolean isActive() {
        return isActive;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Instant getSourceUpdatedAt() {
        return sourceUpdatedAt;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void syncProfile(
            String name,
            String username,
            String avatarUrl,
            boolean isPrivate,
            boolean isActive,
            Instant sourceUpdatedAt
    ) {
        if (isStale(sourceUpdatedAt)) {
            return;
        }

        this.name = name;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.isPrivate = isPrivate;
        this.isActive = isActive;
        this.deletedAt = null;
        markSynced(sourceUpdatedAt);
    }

    public void markDeleted(Instant deletedAt, Instant sourceUpdatedAt) {
        if (isStale(sourceUpdatedAt)) {
            return;
        }

        this.isActive = false;
        this.deletedAt = deletedAt;
        markSynced(sourceUpdatedAt);
    }

    public void restoreFromSource(
            String name,
            String username,
            String avatarUrl,
            boolean isPrivate,
            boolean isActive,
            Instant sourceUpdatedAt
    ) {
        syncProfile(name, username, avatarUrl, isPrivate, isActive, sourceUpdatedAt);
    }

    public void markActive(Instant sourceUpdatedAt) {
        if (isStale(sourceUpdatedAt)) {
            return;
        }

        this.isActive = true;
        markSynced(sourceUpdatedAt);
    }

    public void markInactive(Instant sourceUpdatedAt) {
        if (isStale(sourceUpdatedAt)) {
            return;
        }

        this.isActive = false;
        markSynced(sourceUpdatedAt);
    }

    public boolean canBeShown() {
        return this.isActive && this.deletedAt == null;
    }

    public boolean isOwnedBy(UUID viewerId) {
        return this.userId.equals(viewerId);
    }

    public boolean isPubliclyVisibleTo(UUID viewerId, boolean viewerIsAcceptedFollower, boolean blocked) {
        if (!canBeShown() || blocked) {
            return false;
        }

        return isOwnedBy(viewerId) || !this.isPrivate || viewerIsAcceptedFollower;
    }

    private boolean isStale(Instant incomingSourceUpdatedAt) {
        return incomingSourceUpdatedAt != null
                && this.sourceUpdatedAt != null
                && incomingSourceUpdatedAt.isBefore(this.sourceUpdatedAt);
    }

    private void markSynced(Instant sourceUpdatedAt) {
        Instant now = Instant.now();
        this.sourceUpdatedAt = sourceUpdatedAt;
        this.syncedAt = now;
        this.updatedAt = now;
    }
}
