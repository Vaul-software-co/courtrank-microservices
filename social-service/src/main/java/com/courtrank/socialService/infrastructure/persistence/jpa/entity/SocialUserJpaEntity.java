package com.courtrank.socialService.infrastructure.persistence.jpa.entity;

import com.courtrank.socialService.domain.entity.SocialUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "social_users")
public class SocialUserJpaEntity {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String name;

    private String username;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "private_profile", nullable = false)
    private boolean privateProfile;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "source_updated_at")
    private Instant sourceUpdatedAt;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SocialUserJpaEntity() {
    }

    public SocialUserJpaEntity(
            UUID userId,
            String name,
            String username,
            String avatarUrl,
            boolean privateProfile,
            boolean active,
            Instant deletedAt,
            Instant sourceUpdatedAt,
            Instant syncedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.userId = userId;
        this.name = name;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.privateProfile = privateProfile;
        this.active = active;
        this.deletedAt = deletedAt;
        this.sourceUpdatedAt = sourceUpdatedAt;
        this.syncedAt = syncedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SocialUserJpaEntity fromDomain(SocialUser user) {
        return new SocialUserJpaEntity(
                user.getUserId(),
                user.getName(),
                user.getUsername(),
                user.getAvatarUrl(),
                user.isPrivate(),
                user.isActive(),
                user.getDeletedAt(),
                user.getSourceUpdatedAt(),
                user.getSyncedAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public SocialUser toDomain() {
        return SocialUser.restore(
                this.userId,
                this.name,
                this.username,
                this.avatarUrl,
                this.privateProfile,
                this.active,
                this.deletedAt,
                this.sourceUpdatedAt,
                this.syncedAt,
                this.createdAt,
                this.updatedAt
        );
    }

    public UUID getUserId() {
        return userId;
    }
}
