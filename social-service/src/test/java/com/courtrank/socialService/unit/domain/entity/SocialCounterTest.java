package com.courtrank.socialService.unit.domain.entity;

import com.courtrank.socialService.domain.entity.SocialCounter;
import com.courtrank.socialService.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SocialCounterTest {
    @Test
    void create_shouldInitializeAllCountsAtZero() {
        UUID userId = UUID.randomUUID();

        SocialCounter counter = SocialCounter.create(userId);

        assertThat(counter.getUserId()).isEqualTo(userId);
        assertThat(counter.getFollowersCount()).isZero();
        assertThat(counter.getFollowingCount()).isZero();
        assertThat(counter.getPendingRequestsCount()).isZero();
        assertThat(counter.getBlockedCount()).isZero();
        assertThat(counter.getCreatedAt()).isNotNull();
        assertThat(counter.getUpdatedAt()).isNotNull();
    }

    @Test
    void restore_shouldRoundTripAllStoredFields() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T00:00:00Z");

        SocialCounter counter = SocialCounter.restore(
                userId,
                10,
                5,
                2,
                1,
                createdAt,
                updatedAt
        );

        assertThat(counter.getUserId()).isEqualTo(userId);
        assertThat(counter.getFollowersCount()).isEqualTo(10);
        assertThat(counter.getFollowingCount()).isEqualTo(5);
        assertThat(counter.getPendingRequestsCount()).isEqualTo(2);
        assertThat(counter.getBlockedCount()).isEqualTo(1);
        assertThat(counter.getCreatedAt()).isEqualTo(createdAt);
        assertThat(counter.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void acceptedFollowTransitions_shouldUpdateFollowerAndFollowingCounters() {
        SocialCounter followerCounter = SocialCounter.create(UUID.randomUUID());
        SocialCounter followingCounter = SocialCounter.create(UUID.randomUUID());

        followerCounter.applyAcceptedFollowAsFollower();
        followingCounter.applyAcceptedFollowAsFollowing();

        assertThat(followerCounter.getFollowingCount()).isEqualTo(1);
        assertThat(followingCounter.getFollowersCount()).isEqualTo(1);

        followerCounter.removeAcceptedFollowAsFollower();
        followingCounter.removeAcceptedFollowAsFollowing();

        assertThat(followerCounter.getFollowingCount()).isZero();
        assertThat(followingCounter.getFollowersCount()).isZero();
    }

    @Test
    void pendingRequestTransitions_shouldUpdatePendingAndAcceptedCounts() {
        SocialCounter followerCounter = SocialCounter.create(UUID.randomUUID());
        SocialCounter followingCounter = SocialCounter.create(UUID.randomUUID());

        followingCounter.applyPendingRequestAsFollowing();

        assertThat(followingCounter.getPendingRequestsCount()).isEqualTo(1);

        followerCounter.acceptPendingRequestAsFollower();
        followingCounter.acceptPendingRequestAsFollowing();

        assertThat(followerCounter.getFollowingCount()).isEqualTo(1);
        assertThat(followingCounter.getPendingRequestsCount()).isZero();
        assertThat(followingCounter.getFollowersCount()).isEqualTo(1);
    }

    @Test
    void blockTransitions_shouldUpdateBlockedCountForBlocker() {
        SocialCounter counter = SocialCounter.create(UUID.randomUUID());

        counter.applyBlockAsBlocker();

        assertThat(counter.getBlockedCount()).isEqualTo(1);

        counter.removeBlockAsBlocker();

        assertThat(counter.getBlockedCount()).isZero();
    }

    @Test
    void restore_shouldRejectNegativeCounts() {
        assertThatThrownBy(() -> SocialCounter.restore(
                UUID.randomUUID(),
                -1,
                0,
                0,
                0,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        )).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void decrement_shouldRejectNegativeResult() {
        SocialCounter counter = SocialCounter.create(UUID.randomUUID());

        assertThatThrownBy(counter::decrementFollowers)
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(counter::decrementFollowing)
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(counter::decrementPendingRequests)
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(counter::decrementBlocked)
                .isInstanceOf(DomainValidationException.class);
    }
}
