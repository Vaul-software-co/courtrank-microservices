package com.courtrank.socialService.infrastructure.persistence.jpa.entity;

import com.courtrank.socialService.domain.entity.Follow;
import com.courtrank.socialService.domain.enums.FollowStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "follows")
public class FollowJpaEntity {
    @Id
    private UUID id;

    @Column(name = "follower_id", nullable = false)
    private UUID followerId;

    @Column(name = "following_id", nullable = false)
    private UUID followingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FollowStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FollowJpaEntity() {
    }

    public FollowJpaEntity(UUID id, UUID followerId, UUID followingId, FollowStatus status, Instant createdAt, Instant acceptedAt, Instant updatedAt) {
        this.id = id;
        this.followerId = followerId;
        this.followingId = followingId;
        this.status = status;
        this.createdAt = createdAt;
        this.acceptedAt = acceptedAt;
        this.updatedAt = updatedAt;
    }

    public static FollowJpaEntity fromDomain(Follow follow) {
        return new FollowJpaEntity(
                follow.getId(),
                follow.getFollowerId(),
                follow.getFollowingId(),
                follow.getFollowStatus(),
                follow.getCreatedAt(),
                follow.getAcceptedAt(),
                follow.getUpdatedAt()
        );
    }

    public Follow toDomain() {
        return Follow.restore(this.id, this.followerId, this.followingId, this.status, this.createdAt, this.acceptedAt, this.updatedAt);
    }
}
