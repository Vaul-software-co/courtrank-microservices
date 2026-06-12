package com.courtrank.socialService.infrastructure.persistence.jpa.entity;

import com.courtrank.socialService.domain.entity.SocialCounter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "social_counters")
public class SocialCounterJpaEntity {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "followers_count", nullable = false)
    private int followersCount;

    @Column(name = "following_count", nullable = false)
    private int followingCount;

    @Column(name = "pending_requests_count", nullable = false)
    private int pendingRequestsCount;

    @Column(name = "blocked_count", nullable = false)
    private int blockedCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SocialCounterJpaEntity() {
    }

    public SocialCounterJpaEntity(UUID userId, int followersCount, int followingCount, int pendingRequestsCount, int blockedCount, Instant createdAt, Instant updatedAt) {
        this.userId = userId;
        this.followersCount = followersCount;
        this.followingCount = followingCount;
        this.pendingRequestsCount = pendingRequestsCount;
        this.blockedCount = blockedCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SocialCounterJpaEntity fromDomain(SocialCounter counter) {
        return new SocialCounterJpaEntity(
                counter.getUserId(),
                counter.getFollowersCount(),
                counter.getFollowingCount(),
                counter.getPendingRequestsCount(),
                counter.getBlockedCount(),
                counter.getCreatedAt(),
                counter.getUpdatedAt()
        );
    }

    public SocialCounter toDomain() {
        return SocialCounter.restore(this.userId, this.followersCount, this.followingCount, this.pendingRequestsCount, this.blockedCount, this.createdAt, this.updatedAt);
    }
}
