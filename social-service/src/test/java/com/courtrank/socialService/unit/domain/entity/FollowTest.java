package com.courtrank.socialService.unit.domain.entity;

import com.courtrank.socialService.domain.entity.Follow;
import com.courtrank.socialService.domain.entity.SocialUser;
import com.courtrank.socialService.domain.enums.FollowStatus;
import com.courtrank.socialService.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FollowTest {
    @Test
    void request_shouldCreatePendingFollowWithoutAcceptedAt() {
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();
        SocialUser privateTarget = SocialUser.create(
                followingId,
                "Target",
                "target",
                null,
                true,
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );

        Follow follow = Follow.startFollowing(followerId, privateTarget);

        assertThat(follow.getId()).isNotNull();
        assertThat(follow.getFollowerId()).isEqualTo(followerId);
        assertThat(follow.getFollowingId()).isEqualTo(followingId);
        assertThat(follow.getFollowStatus()).isEqualTo(FollowStatus.PENDING);
        assertThat(follow.isPending()).isTrue();
        assertThat(follow.isAccepted()).isFalse();
        assertThat(follow.getAcceptedAt()).isNull();
        assertThat(follow.getCreatedAt()).isNotNull();
        assertThat(follow.getUpdatedAt()).isNotNull();
    }

    @Test
    void accepted_shouldCreateAcceptedFollowWithAcceptedAt() {
        SocialUser publicTarget = SocialUser.create(
                UUID.randomUUID(),
                "Target",
                "target",
                null,
                false,
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );

        Follow follow = Follow.startFollowing(UUID.randomUUID(), publicTarget);

        assertThat(follow.getFollowStatus()).isEqualTo(FollowStatus.ACCEPTED);
        assertThat(follow.isAccepted()).isTrue();
        assertThat(follow.getAcceptedAt()).isNotNull();
    }

    @Test
    void startFollowing_shouldUsePendingForPrivateTargetAndAcceptedForPublicTarget() {
        UUID followerId = UUID.randomUUID();
        SocialUser privateTarget = SocialUser.create(
                UUID.randomUUID(),
                "Private Target",
                "private_target",
                null,
                true,
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        SocialUser publicTarget = SocialUser.create(
                UUID.randomUUID(),
                "Public Target",
                "public_target",
                null,
                false,
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );

        Follow pending = Follow.startFollowing(followerId, privateTarget);
        Follow accepted = Follow.startFollowing(followerId, publicTarget);

        assertThat(pending.getFollowStatus()).isEqualTo(FollowStatus.PENDING);
        assertThat(pending.getAcceptedAt()).isNull();
        assertThat(accepted.getFollowStatus()).isEqualTo(FollowStatus.ACCEPTED);
        assertThat(accepted.getAcceptedAt()).isNotNull();
    }

    @Test
    void restore_shouldRoundTripAllStoredFields() {
        UUID id = UUID.randomUUID();
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant acceptedAt = Instant.parse("2026-01-02T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-03T00:00:00Z");

        Follow follow = Follow.restore(
                id,
                followerId,
                followingId,
                FollowStatus.ACCEPTED,
                createdAt,
                acceptedAt,
                updatedAt
        );

        assertThat(follow.getId()).isEqualTo(id);
        assertThat(follow.getFollowerId()).isEqualTo(followerId);
        assertThat(follow.getFollowingId()).isEqualTo(followingId);
        assertThat(follow.getFollowStatus()).isEqualTo(FollowStatus.ACCEPTED);
        assertThat(follow.getCreatedAt()).isEqualTo(createdAt);
        assertThat(follow.getAcceptedAt()).isEqualTo(acceptedAt);
        assertThat(follow.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void accept_shouldOnlyAllowFollowedUserToAcceptRequest() {
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();
        SocialUser privateTarget = SocialUser.create(
                followingId,
                "Target",
                "target",
                null,
                true,
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        Follow follow = Follow.startFollowing(followerId, privateTarget);

        assertThatThrownBy(() -> follow.accept(followerId))
                .isInstanceOf(DomainValidationException.class);

        follow.accept(followingId);

        assertThat(follow.getFollowStatus()).isEqualTo(FollowStatus.ACCEPTED);
        assertThat(follow.getAcceptedAt()).isNotNull();
    }

    @Test
    void create_shouldRejectSelfFollow() {
        UUID userId = UUID.randomUUID();
        SocialUser target = SocialUser.create(
                userId,
                "Target",
                "target",
                null,
                false,
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );

        assertThatThrownBy(() -> Follow.startFollowing(userId, target))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void startFollowing_shouldRejectInactiveOrDeletedTarget() {
        UUID followerId = UUID.randomUUID();
        SocialUser target = SocialUser.create(
                UUID.randomUUID(),
                "Target",
                "target",
                null,
                false,
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        target.markDeleted(
                Instant.parse("2026-01-02T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z")
        );

        assertThatThrownBy(() -> Follow.startFollowing(followerId, target))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void helpers_shouldIdentifyOwnersAndRelatedUsers() {
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        SocialUser privateTarget = SocialUser.create(
                followingId,
                "Target",
                "target",
                null,
                true,
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        Follow follow = Follow.startFollowing(followerId, privateTarget);

        assertThat(follow.isOwnedByFollower(followerId)).isTrue();
        assertThat(follow.isOwnedByFollowing(followingId)).isTrue();
        assertThat(follow.isBetween(followerId, followingId)).isTrue();
        assertThat(follow.isBetween(followerId, otherId)).isFalse();
    }

    @Test
    void pendingFollow_shouldOnlyBeCancelableByFollower() {
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();
        SocialUser privateTarget = SocialUser.create(
                followingId,
                "Target",
                "target",
                null,
                true,
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        Follow follow = Follow.startFollowing(followerId, privateTarget);

        assertThat(follow.canBeCanceledBy(followerId)).isTrue();
        assertThat(follow.canBeCanceledBy(followingId)).isFalse();
        assertThat(follow.canBeUnfollowedBy(followerId)).isFalse();
        assertThat(follow.canBeRemovedBy(followingId)).isFalse();
        assertThatThrownBy(() -> follow.assertCanBeCanceledBy(followingId))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void acceptedFollow_shouldBeUnfollowableByFollowerAndRemovableByFollowing() {
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();
        SocialUser publicTarget = SocialUser.create(
                followingId,
                "Target",
                "target",
                null,
                false,
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        Follow follow = Follow.startFollowing(followerId, publicTarget);

        assertThat(follow.canBeUnfollowedBy(followerId)).isTrue();
        assertThat(follow.canBeRemovedBy(followingId)).isTrue();
        assertThat(follow.canBeCanceledBy(followerId)).isFalse();
        assertThatThrownBy(() -> follow.assertCanBeUnfollowedBy(followingId))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> follow.assertCanBeRemovedBy(followerId))
                .isInstanceOf(DomainValidationException.class);
    }
}
