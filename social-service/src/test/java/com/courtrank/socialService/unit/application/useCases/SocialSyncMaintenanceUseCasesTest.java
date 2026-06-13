package com.courtrank.socialService.unit.application.useCases;

import com.courtrank.socialService.application.dto.DeleteSocialUserRequest;
import com.courtrank.socialService.application.dto.RebuildSocialCounterRequest;
import com.courtrank.socialService.application.dto.ReconcileSocialUserRequest;
import com.courtrank.socialService.application.dto.SocialUserSnapshot;
import com.courtrank.socialService.application.dto.SyncSocialUserRequest;
import com.courtrank.socialService.application.events.FollowAcceptedEvent;
import com.courtrank.socialService.application.events.FollowRejectedEvent;
import com.courtrank.socialService.application.events.FollowRemovedEvent;
import com.courtrank.socialService.application.events.FollowRequestedEvent;
import com.courtrank.socialService.application.events.FollowerRemovedEvent;
import com.courtrank.socialService.application.events.UserBlockedEvent;
import com.courtrank.socialService.application.events.UserUnblockedEvent;
import com.courtrank.socialService.application.ports.SocialEventPublisher;
import com.courtrank.socialService.application.ports.SocialUserProfileProvider;
import com.courtrank.socialService.application.useCases.CreateSocialUserFromUserEventUseCase;
import com.courtrank.socialService.application.useCases.DeleteSocialUserFromUserEventUseCase;
import com.courtrank.socialService.application.useCases.HandleUserBecamePublicUseCase;
import com.courtrank.socialService.application.useCases.RebuildAllSocialCountersUseCase;
import com.courtrank.socialService.application.useCases.RebuildSocialCounterUseCase;
import com.courtrank.socialService.application.useCases.ReconcileSocialUserUseCase;
import com.courtrank.socialService.application.useCases.RestoreSocialUserFromUserEventUseCase;
import com.courtrank.socialService.application.useCases.UpdateSocialUserFromUserEventUseCase;
import com.courtrank.socialService.domain.entity.Block;
import com.courtrank.socialService.domain.entity.Follow;
import com.courtrank.socialService.domain.entity.SocialCounter;
import com.courtrank.socialService.domain.entity.SocialUser;
import com.courtrank.socialService.domain.enums.FollowStatus;
import com.courtrank.socialService.domain.repository.BlockRepository;
import com.courtrank.socialService.domain.repository.FollowRepository;
import com.courtrank.socialService.domain.repository.SocialCounterRepository;
import com.courtrank.socialService.domain.repository.SocialUserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SocialSyncMaintenanceUseCasesTest {
    @Test
    void createFromUserEvent_shouldCreateSocialUserAndCounter() {
        Fixture fx = new Fixture();
        UUID userId = UUID.randomUUID();

        new CreateSocialUserFromUserEventUseCase(fx.users, fx.counters)
                .execute(new SyncSocialUserRequest(snapshot(userId, "Ana", "ana", false, true, Instant.parse("2026-01-01T00:00:00Z"))));

        SocialUser user = fx.users.findByUserId(userId).orElseThrow();
        assertThat(user.getName()).isEqualTo("Ana");
        assertThat(user.getUsername()).isEqualTo("ana");
        assertThat(user.isPrivate()).isFalse();
        assertThat(fx.counters.findByUserId(userId)).isPresent();
    }

    @Test
    void deleteAndRestoreFromUserEvents_shouldHideAndThenShowProfileAgain() {
        Fixture fx = new Fixture();
        UUID userId = UUID.randomUUID();
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();
        UUID pendingRequesterId = UUID.randomUUID();
        fx.users.save(user(userId, "Old", "old", false, true, Instant.parse("2026-01-01T00:00:00Z")));
        fx.follows.save(follow(followerId, userId, FollowStatus.ACCEPTED));
        fx.follows.save(follow(userId, followingId, FollowStatus.ACCEPTED));
        fx.follows.save(follow(pendingRequesterId, userId, FollowStatus.PENDING));
        SocialCounter userCounter = SocialCounter.create(userId);
        userCounter.applyAcceptedFollowAsFollowing();
        userCounter.applyAcceptedFollowAsFollower();
        userCounter.applyPendingRequestAsFollowing();
        fx.counters.save(userCounter);
        SocialCounter followerCounter = SocialCounter.create(followerId);
        followerCounter.applyAcceptedFollowAsFollower();
        fx.counters.save(followerCounter);
        SocialCounter followingCounter = SocialCounter.create(followingId);
        followingCounter.applyAcceptedFollowAsFollowing();
        fx.counters.save(followingCounter);

        new DeleteSocialUserFromUserEventUseCase(fx.users, fx.follows, fx.counters)
                .execute(new DeleteSocialUserRequest(
                        userId,
                        Instant.parse("2026-01-02T00:00:00Z"),
                        Instant.parse("2026-01-02T00:00:00Z")
                ));

        assertThat(fx.users.findByUserId(userId).orElseThrow().canBeShown()).isFalse();
        assertThat(fx.follows.findFollowersByFollowingId(userId)).isEmpty();
        assertThat(fx.follows.findFollowingByFollowerId(userId)).isEmpty();
        assertThat(fx.counters.findByUserId(userId).orElseThrow().getFollowersCount()).isZero();
        assertThat(fx.counters.findByUserId(userId).orElseThrow().getFollowingCount()).isZero();
        assertThat(fx.counters.findByUserId(userId).orElseThrow().getPendingRequestsCount()).isZero();
        assertThat(fx.counters.findByUserId(followerId).orElseThrow().getFollowingCount()).isZero();
        assertThat(fx.counters.findByUserId(followingId).orElseThrow().getFollowersCount()).isZero();

        new RestoreSocialUserFromUserEventUseCase(fx.users, fx.counters)
                .execute(new SyncSocialUserRequest(snapshot(userId, "New", "new", true, true, Instant.parse("2026-01-03T00:00:00Z"))));

        SocialUser restored = fx.users.findByUserId(userId).orElseThrow();
        assertThat(restored.canBeShown()).isTrue();
        assertThat(restored.getName()).isEqualTo("New");
        assertThat(restored.isPrivate()).isTrue();
        assertThat(fx.counters.findByUserId(userId)).isPresent();
        assertThat(fx.follows.findFollowersByFollowingId(userId)).isEmpty();
        assertThat(fx.follows.findFollowingByFollowerId(userId)).isEmpty();
    }

    @Test
    void updateFromUserEvent_shouldAcceptPendingFollowsWhenUserBecomesPublic() {
        Fixture fx = new Fixture();
        UUID followerId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        fx.users.save(user(targetId, "Target", "target", true, true, Instant.parse("2026-01-01T00:00:00Z")));
        Follow pending = fx.follows.save(Follow.startFollowing(followerId, fx.users.findByUserId(targetId).orElseThrow()));
        fx.counters.save(SocialCounter.create(followerId));
        SocialCounter targetCounter = SocialCounter.create(targetId);
        targetCounter.applyPendingRequestAsFollowing();
        fx.counters.save(targetCounter);

        new UpdateSocialUserFromUserEventUseCase(fx.users, fx.counters, fx.follows, fx.events)
                .execute(new SyncSocialUserRequest(snapshot(targetId, "Target", "target", false, true, Instant.parse("2026-01-02T00:00:00Z"))));

        assertThat(fx.follows.findById(pending.getId()).orElseThrow().isAccepted()).isTrue();
        assertThat(fx.counters.findByUserId(followerId).orElseThrow().getFollowingCount()).isEqualTo(1);
        assertThat(fx.counters.findByUserId(targetId).orElseThrow().getFollowersCount()).isEqualTo(1);
        assertThat(fx.counters.findByUserId(targetId).orElseThrow().getPendingRequestsCount()).isZero();
        assertThat(fx.events.followAcceptedEvents).hasSize(1);
    }

    @Test
    void handleUserBecamePublic_shouldAcceptAllPendingFollows() {
        Fixture fx = new Fixture();
        UUID firstFollowerId = UUID.randomUUID();
        UUID secondFollowerId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        SocialUser privateTarget = user(targetId, "Target", "target", true, true, Instant.parse("2026-01-01T00:00:00Z"));
        fx.follows.save(Follow.startFollowing(firstFollowerId, privateTarget));
        fx.follows.save(Follow.startFollowing(secondFollowerId, privateTarget));
        fx.counters.save(SocialCounter.create(firstFollowerId));
        fx.counters.save(SocialCounter.create(secondFollowerId));
        SocialCounter targetCounter = SocialCounter.create(targetId);
        targetCounter.applyPendingRequestAsFollowing();
        targetCounter.applyPendingRequestAsFollowing();
        fx.counters.save(targetCounter);

        new HandleUserBecamePublicUseCase(fx.follows, fx.counters, fx.events).execute(targetId);

        assertThat(fx.follows.findPendingByFollowingId(targetId)).isEmpty();
        assertThat(fx.counters.findByUserId(targetId).orElseThrow().getFollowersCount()).isEqualTo(2);
        assertThat(fx.counters.findByUserId(targetId).orElseThrow().getPendingRequestsCount()).isZero();
        assertThat(fx.events.followAcceptedEvents).hasSize(2);
    }

    @Test
    void rebuildSocialCounter_shouldRecalculateFromFollowsAndBlocks() {
        Fixture fx = new Fixture();
        UUID userId = UUID.randomUUID();
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        fx.follows.save(follow(followerId, userId, FollowStatus.ACCEPTED));
        fx.follows.save(follow(userId, followingId, FollowStatus.ACCEPTED));
        fx.follows.save(follow(requesterId, userId, FollowStatus.PENDING));
        fx.blocks.save(Block.restore(UUID.randomUUID(), userId, blockedId, Instant.parse("2026-01-01T00:00:00Z")));

        SocialCounter rebuilt = new RebuildSocialCounterUseCase(fx.follows, fx.blocks, fx.counters)
                .execute(new RebuildSocialCounterRequest(userId));

        assertThat(rebuilt.getFollowersCount()).isEqualTo(1);
        assertThat(rebuilt.getFollowingCount()).isEqualTo(1);
        assertThat(rebuilt.getPendingRequestsCount()).isEqualTo(1);
        assertThat(rebuilt.getBlockedCount()).isEqualTo(1);
        assertThat(fx.counters.findByUserId(userId)).contains(rebuilt);
    }

    @Test
    void rebuildAllSocialCounters_shouldRebuildEveryKnownSocialUser() {
        Fixture fx = new Fixture();
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        fx.users.save(user(firstUserId, "First", "first", false, true, Instant.parse("2026-01-01T00:00:00Z")));
        fx.users.save(user(secondUserId, "Second", "second", false, true, Instant.parse("2026-01-01T00:00:00Z")));

        List<SocialCounter> rebuilt = new RebuildAllSocialCountersUseCase(fx.users, fx.follows, fx.blocks, fx.counters).execute();

        assertThat(rebuilt).extracting(SocialCounter::getUserId).containsExactlyInAnyOrder(firstUserId, secondUserId);
        assertThat(fx.counters.findByUserId(firstUserId)).isPresent();
        assertThat(fx.counters.findByUserId(secondUserId)).isPresent();
    }

    @Test
    void reconcileSocialUser_shouldPullSnapshotFromUserServicePortAndSyncProfile() {
        Fixture fx = new Fixture();
        UUID userId = UUID.randomUUID();
        SocialUserSnapshot snapshot = snapshot(userId, "From User", "from_user", false, true, Instant.parse("2026-01-01T00:00:00Z"));
        SocialUserProfileProvider provider = requestedUserId -> requestedUserId.equals(userId) ? Optional.of(snapshot) : Optional.empty();
        UpdateSocialUserFromUserEventUseCase updateUseCase = new UpdateSocialUserFromUserEventUseCase(fx.users, fx.counters, fx.follows, fx.events);

        new ReconcileSocialUserUseCase(provider, updateUseCase).execute(new ReconcileSocialUserRequest(userId));

        assertThat(fx.users.findByUserId(userId).orElseThrow().getName()).isEqualTo("From User");
        assertThat(fx.counters.findByUserId(userId)).isPresent();
    }

    @Test
    void reconcileSocialUser_shouldFailWhenSourceUserDoesNotExist() {
        Fixture fx = new Fixture();
        SocialUserProfileProvider provider = userId -> Optional.empty();
        UpdateSocialUserFromUserEventUseCase updateUseCase = new UpdateSocialUserFromUserEventUseCase(fx.users, fx.counters, fx.follows, fx.events);

        assertThatThrownBy(() -> new ReconcileSocialUserUseCase(provider, updateUseCase)
                .execute(new ReconcileSocialUserRequest(UUID.randomUUID())))
                .isInstanceOf(RuntimeException.class);
    }

    private static SocialUserSnapshot snapshot(
            UUID userId,
            String name,
            String username,
            boolean isPrivate,
            boolean isActive,
            Instant sourceUpdatedAt
    ) {
        return new SocialUserSnapshot(userId, name, username, null, isPrivate, isActive, null, sourceUpdatedAt);
    }

    private static SocialUser user(
            UUID userId,
            String name,
            String username,
            boolean isPrivate,
            boolean isActive,
            Instant sourceUpdatedAt
    ) {
        return SocialUser.create(userId, name, username, null, isPrivate, isActive, sourceUpdatedAt);
    }

    private static Follow follow(UUID followerId, UUID followingId, FollowStatus status) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return Follow.restore(UUID.randomUUID(), followerId, followingId, status, now, status == FollowStatus.ACCEPTED ? now : null, now);
    }

    private static String followKey(UUID followerId, UUID followingId) {
        return followerId + ":" + followingId;
    }

    private static class Fixture {
        final InMemoryFollowRepository follows = new InMemoryFollowRepository();
        final InMemoryBlockRepository blocks = new InMemoryBlockRepository();
        final InMemorySocialUserRepository users = new InMemorySocialUserRepository();
        final InMemorySocialCounterRepository counters = new InMemorySocialCounterRepository();
        final RecordingSocialEventPublisher events = new RecordingSocialEventPublisher();
    }

    private static class InMemoryFollowRepository implements FollowRepository {
        private final Map<String, Follow> rows = new HashMap<>();

        @Override public Follow save(Follow follow) { this.rows.put(followKey(follow.getFollowerId(), follow.getFollowingId()), follow); return follow; }
        @Override public Optional<Follow> findById(UUID id) { return this.rows.values().stream().filter(follow -> follow.getId().equals(id)).findFirst(); }
        @Override public Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId) { return Optional.ofNullable(this.rows.get(followKey(followerId, followingId))); }
        @Override public boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId) { return this.rows.containsKey(followKey(followerId, followingId)); }
        @Override public boolean existsAcceptedByFollowerIdAndFollowingId(UUID followerId, UUID followingId) { return findByFollowerIdAndFollowingId(followerId, followingId).map(Follow::isAccepted).orElse(false); }
        @Override public List<Follow> findFollowersByFollowingId(UUID followingId) { return this.rows.values().stream().filter(follow -> follow.getFollowingId().equals(followingId)).toList(); }
        @Override public List<Follow> findFollowingByFollowerId(UUID followerId) { return this.rows.values().stream().filter(follow -> follow.getFollowerId().equals(followerId)).toList(); }
        @Override public List<Follow> findPendingByFollowingId(UUID followingId) { return this.rows.values().stream().filter(follow -> follow.getFollowingId().equals(followingId)).filter(Follow::isPending).toList(); }
        @Override public List<Follow> findBetweenUsers(UUID userA, UUID userB) { return this.rows.values().stream().filter(follow -> follow.isBetween(userA, userB)).toList(); }
        @Override public void delete(Follow follow) { this.rows.remove(followKey(follow.getFollowerId(), follow.getFollowingId())); }
    }

    private static class InMemoryBlockRepository implements BlockRepository {
        private final Map<String, Block> rows = new HashMap<>();

        @Override public Block save(Block block) { this.rows.put(followKey(block.getBlockerId(), block.getBlockedId()), block); return block; }
        @Override public Optional<Block> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId) { return Optional.ofNullable(this.rows.get(followKey(blockerId, blockedId))); }
        @Override public List<Block> findByBlockerId(UUID blockerId) { return this.rows.values().stream().filter(block -> block.getBlockerId().equals(blockerId)).toList(); }
        @Override public boolean existsBetweenUsers(UUID userA, UUID userB) { return this.rows.values().stream().anyMatch(block -> block.isBetween(userA, userB)); }
        @Override public Set<UUID> findRelatedUserIds(UUID userId) { return this.rows.values().stream().filter(block -> block.involves(userId)).map(block -> block.getBlockerId().equals(userId) ? block.getBlockedId() : block.getBlockerId()).collect(Collectors.toSet()); }
        @Override public void delete(Block block) { this.rows.remove(followKey(block.getBlockerId(), block.getBlockedId())); }
    }

    private static class InMemorySocialUserRepository implements SocialUserRepository {
        private final Map<UUID, SocialUser> rows = new HashMap<>();

        @Override public void save(SocialUser socialUser) { this.rows.put(socialUser.getUserId(), socialUser); }
        @Override public Optional<SocialUser> findByUserId(UUID userId) { return Optional.ofNullable(this.rows.get(userId)); }
        @Override public List<UUID> findAllUserIds() { return List.copyOf(this.rows.keySet()); }
        @Override public List<SocialUser> searchVisible(String query, int limit, Set<UUID> excludedUserIds) { return this.rows.values().stream().filter(SocialUser::canBeShown).filter(user -> !excludedUserIds.contains(user.getUserId())).limit(limit).toList(); }
    }

    private static class InMemorySocialCounterRepository implements SocialCounterRepository {
        private final Map<UUID, SocialCounter> rows = new HashMap<>();

        @Override public SocialCounter save(SocialCounter socialCounter) { this.rows.put(socialCounter.getUserId(), socialCounter); return socialCounter; }
        @Override public Optional<SocialCounter> findByUserId(UUID userId) { return Optional.ofNullable(this.rows.get(userId)); }
    }

    private static class RecordingSocialEventPublisher implements SocialEventPublisher {
        final List<FollowAcceptedEvent> followAcceptedEvents = new ArrayList<>();

        @Override public void publishFollowRequested(FollowRequestedEvent event) {}
        @Override public void publishFollowAccepted(FollowAcceptedEvent event) { this.followAcceptedEvents.add(event); }
        @Override public void publishFollowRejected(FollowRejectedEvent event) {}
        @Override public void publishFollowRemoved(FollowRemovedEvent event) {}
        @Override public void publishFollowerRemoved(FollowerRemovedEvent event) {}
        @Override public void publishUserBlocked(UserBlockedEvent event) {}
        @Override public void publishUserUnblocked(UserUnblockedEvent event) {}
    }
}
