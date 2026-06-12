package com.courtrank.socialService.unit.application.useCases;

import com.courtrank.socialService.application.dto.AcceptFollowRequestRequest;
import com.courtrank.socialService.application.dto.BlockUserRequest;
import com.courtrank.socialService.application.dto.RemoveFollowerRequest;
import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.application.dto.UnblockUserRequest;
import com.courtrank.socialService.application.dto.UnfollowUserRequest;
import com.courtrank.socialService.application.events.FollowAcceptedEvent;
import com.courtrank.socialService.application.events.FollowRejectedEvent;
import com.courtrank.socialService.application.events.FollowRemovedEvent;
import com.courtrank.socialService.application.events.FollowRequestedEvent;
import com.courtrank.socialService.application.events.FollowerRemovedEvent;
import com.courtrank.socialService.application.events.UserBlockedEvent;
import com.courtrank.socialService.application.events.UserUnblockedEvent;
import com.courtrank.socialService.application.ports.SocialEventPublisher;
import com.courtrank.socialService.application.ports.audit.SocialAuditEvent;
import com.courtrank.socialService.application.ports.audit.SocialAuditLogger;
import com.courtrank.socialService.application.useCases.AcceptFollowRequestUseCase;
import com.courtrank.socialService.application.useCases.BlockUserUseCase;
import com.courtrank.socialService.application.useCases.RemoveFollowerUseCase;
import com.courtrank.socialService.application.useCases.UnblockUserUseCase;
import com.courtrank.socialService.application.useCases.UnfollowUserUseCase;
import com.courtrank.socialService.domain.entity.Block;
import com.courtrank.socialService.domain.entity.Follow;
import com.courtrank.socialService.domain.entity.SocialCounter;
import com.courtrank.socialService.domain.entity.SocialUser;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SocialMutationUseCasesTest {
    @Test
    void acceptFollowRequest_shouldUpdateFollowAndCounters() {
        Fixture fx = new Fixture();
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();
        Follow follow = fx.follows.save(Follow.startFollowing(followerId, user(followingId, true)));
        fx.counters.save(SocialCounter.create(followerId));
        SocialCounter followingCounter = SocialCounter.create(followingId);
        followingCounter.applyPendingRequestAsFollowing();
        fx.counters.save(followingCounter);

        new AcceptFollowRequestUseCase(fx.follows, fx.counters, fx.events, fx.audit)
                .execute(new AcceptFollowRequestRequest(followingId, follow.getId()), TraceContext.fromRequestId("req-1"));

        assertThat(fx.follows.findById(follow.getId()).orElseThrow().isAccepted()).isTrue();
        assertThat(fx.counters.findByUserId(followerId).orElseThrow().getFollowingCount()).isEqualTo(1);
        assertThat(fx.counters.findByUserId(followingId).orElseThrow().getFollowersCount()).isEqualTo(1);
        assertThat(fx.counters.findByUserId(followingId).orElseThrow().getPendingRequestsCount()).isZero();
        assertThat(fx.events.followAcceptedEvents).hasSize(1);
    }

    @Test
    void unfollowAcceptedFollow_shouldDeleteFollowAndUpdateCounters() {
        Fixture fx = new Fixture();
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();
        Follow follow = fx.follows.save(Follow.startFollowing(followerId, user(followingId, false)));
        SocialCounter followerCounter = SocialCounter.create(followerId);
        followerCounter.applyAcceptedFollowAsFollower();
        fx.counters.save(followerCounter);
        SocialCounter followingCounter = SocialCounter.create(followingId);
        followingCounter.applyAcceptedFollowAsFollowing();
        fx.counters.save(followingCounter);

        new UnfollowUserUseCase(fx.follows, fx.counters, fx.events, fx.audit)
                .execute(new UnfollowUserRequest(followerId, followingId), null);

        assertThat(fx.follows.findById(follow.getId())).isEmpty();
        assertThat(fx.counters.findByUserId(followerId).orElseThrow().getFollowingCount()).isZero();
        assertThat(fx.counters.findByUserId(followingId).orElseThrow().getFollowersCount()).isZero();
        assertThat(fx.events.followRemovedEvents).hasSize(1);
    }

    @Test
    void removeFollower_shouldDeleteAcceptedFollowAndUpdateCounters() {
        Fixture fx = new Fixture();
        UUID followerId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        fx.follows.save(Follow.startFollowing(followerId, user(ownerId, false)));
        SocialCounter followerCounter = SocialCounter.create(followerId);
        followerCounter.applyAcceptedFollowAsFollower();
        fx.counters.save(followerCounter);
        SocialCounter ownerCounter = SocialCounter.create(ownerId);
        ownerCounter.applyAcceptedFollowAsFollowing();
        fx.counters.save(ownerCounter);

        new RemoveFollowerUseCase(fx.follows, fx.counters, fx.events, fx.audit)
                .execute(new RemoveFollowerRequest(ownerId, followerId), null);

        assertThat(fx.follows.findByFollowerIdAndFollowingId(followerId, ownerId)).isEmpty();
        assertThat(fx.counters.findByUserId(followerId).orElseThrow().getFollowingCount()).isZero();
        assertThat(fx.counters.findByUserId(ownerId).orElseThrow().getFollowersCount()).isZero();
        assertThat(fx.events.followerRemovedEvents).hasSize(1);
    }

    @Test
    void blockUser_shouldCreateBlockRemoveRelatedFollowsAndUpdateCounters() {
        Fixture fx = new Fixture();
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        fx.users.save(user(blockerId, false));
        fx.users.save(user(blockedId, false));
        fx.follows.save(Follow.startFollowing(blockerId, user(blockedId, false)));
        fx.follows.save(Follow.startFollowing(blockedId, user(blockerId, false)));
        SocialCounter blockerCounter = SocialCounter.create(blockerId);
        blockerCounter.applyAcceptedFollowAsFollower();
        blockerCounter.applyAcceptedFollowAsFollowing();
        fx.counters.save(blockerCounter);
        SocialCounter blockedCounter = SocialCounter.create(blockedId);
        blockedCounter.applyAcceptedFollowAsFollower();
        blockedCounter.applyAcceptedFollowAsFollowing();
        fx.counters.save(blockedCounter);

        new BlockUserUseCase(fx.blocks, fx.follows, fx.users, fx.counters, fx.events, fx.audit)
                .execute(new BlockUserRequest(blockerId, blockedId), null);

        assertThat(fx.blocks.existsBetweenUsers(blockerId, blockedId)).isTrue();
        assertThat(fx.follows.findBetweenUsers(blockerId, blockedId)).isEmpty();
        assertThat(fx.counters.findByUserId(blockerId).orElseThrow().getBlockedCount()).isEqualTo(1);
        assertThat(fx.counters.findByUserId(blockerId).orElseThrow().getFollowersCount()).isZero();
        assertThat(fx.counters.findByUserId(blockerId).orElseThrow().getFollowingCount()).isZero();
        assertThat(fx.events.userBlockedEvents).hasSize(1);
    }

    @Test
    void unblockUser_shouldRemoveBlockAndUpdateCounter() {
        Fixture fx = new Fixture();
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        Block block = fx.blocks.save(Block.blockUser(blockerId, user(blockedId, false)));
        SocialCounter counter = SocialCounter.create(blockerId);
        counter.applyBlockAsBlocker();
        fx.counters.save(counter);

        new UnblockUserUseCase(fx.blocks, fx.counters, fx.events, fx.audit)
                .execute(new UnblockUserRequest(blockerId, blockedId), null);

        assertThat(fx.blocks.findByBlockerIdAndBlockedId(blockerId, blockedId)).isEmpty();
        assertThat(fx.counters.findByUserId(blockerId).orElseThrow().getBlockedCount()).isZero();
        assertThat(fx.events.userUnblockedEvents).extracting(UserUnblockedEvent::blockId).containsExactly(block.getId());
    }

    private static SocialUser user(UUID id, boolean isPrivate) {
        return SocialUser.create(id, "User", "user", null, isPrivate, true, Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static String followKey(UUID followerId, UUID followingId) {
        return followerId + ":" + followingId;
    }

    private static String pairKey(UUID userA, UUID userB) {
        return userA.compareTo(userB) < 0 ? userA + ":" + userB : userB + ":" + userA;
    }

    private static class Fixture {
        final InMemoryFollowRepository follows = new InMemoryFollowRepository();
        final InMemoryBlockRepository blocks = new InMemoryBlockRepository();
        final InMemorySocialUserRepository users = new InMemorySocialUserRepository();
        final InMemorySocialCounterRepository counters = new InMemorySocialCounterRepository();
        final RecordingSocialEventPublisher events = new RecordingSocialEventPublisher();
        final RecordingSocialAuditLogger audit = new RecordingSocialAuditLogger();
    }

    private static class InMemoryFollowRepository implements FollowRepository {
        private final Map<String, Follow> rows = new HashMap<>();

        @Override public Follow save(Follow follow) { rows.put(followKey(follow.getFollowerId(), follow.getFollowingId()), follow); return follow; }
        @Override public Optional<Follow> findById(UUID id) { return rows.values().stream().filter(f -> f.getId().equals(id)).findFirst(); }
        @Override public Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId) { return Optional.ofNullable(rows.get(followKey(followerId, followingId))); }
        @Override public boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId) { return rows.containsKey(followKey(followerId, followingId)); }
        @Override public boolean existsAcceptedByFollowerIdAndFollowingId(UUID followerId, UUID followingId) { return findByFollowerIdAndFollowingId(followerId, followingId).map(Follow::isAccepted).orElse(false); }
        @Override public List<Follow> findFollowersByFollowingId(UUID followingId) { return rows.values().stream().filter(f -> f.getFollowingId().equals(followingId)).toList(); }
        @Override public List<Follow> findFollowingByFollowerId(UUID followerId) { return rows.values().stream().filter(f -> f.getFollowerId().equals(followerId)).toList(); }
        @Override public List<Follow> findPendingByFollowingId(UUID followingId) { return rows.values().stream().filter(f -> f.getFollowingId().equals(followingId)).filter(Follow::isPending).toList(); }
        @Override public List<Follow> findBetweenUsers(UUID userA, UUID userB) { return rows.values().stream().filter(f -> f.isBetween(userA, userB)).toList(); }
        @Override public void delete(Follow follow) { rows.remove(followKey(follow.getFollowerId(), follow.getFollowingId())); }
    }

    private static class InMemoryBlockRepository implements BlockRepository {
        private final Map<String, Block> rows = new HashMap<>();

        @Override public Block save(Block block) { rows.put(followKey(block.getBlockerId(), block.getBlockedId()), block); return block; }
        @Override public Optional<Block> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId) { return Optional.ofNullable(rows.get(followKey(blockerId, blockedId))); }
        @Override public List<Block> findByBlockerId(UUID blockerId) { return rows.values().stream().filter(block -> block.getBlockerId().equals(blockerId)).toList(); }
        @Override public boolean existsBetweenUsers(UUID userA, UUID userB) { return rows.values().stream().anyMatch(b -> b.isBetween(userA, userB)); }
        @Override public java.util.Set<UUID> findRelatedUserIds(UUID userId) { return rows.values().stream().filter(b -> b.involves(userId)).map(b -> b.getBlockerId().equals(userId) ? b.getBlockedId() : b.getBlockerId()).collect(java.util.stream.Collectors.toSet()); }
        @Override public void delete(Block block) { rows.remove(followKey(block.getBlockerId(), block.getBlockedId())); }
    }

    private static class InMemorySocialUserRepository implements SocialUserRepository {
        private final Map<UUID, SocialUser> rows = new HashMap<>();
        @Override public void save(SocialUser socialUser) { rows.put(socialUser.getUserId(), socialUser); }
        @Override public Optional<SocialUser> findByUserId(UUID userId) { return Optional.ofNullable(rows.get(userId)); }
        @Override public List<UUID> findAllUserIds() { return List.copyOf(rows.keySet()); }
        @Override public List<SocialUser> searchVisible(String query, int limit, java.util.Set<UUID> excludedUserIds) { return rows.values().stream().filter(SocialUser::canBeShown).filter(user -> !excludedUserIds.contains(user.getUserId())).filter(user -> user.getName().toLowerCase().contains(query.toLowerCase()) || user.getUsername().toLowerCase().contains(query.toLowerCase())).limit(limit).toList(); }
    }

    private static class InMemorySocialCounterRepository implements SocialCounterRepository {
        private final Map<UUID, SocialCounter> rows = new HashMap<>();
        @Override public SocialCounter save(SocialCounter socialCounter) { rows.put(socialCounter.getUserId(), socialCounter); return socialCounter; }
        @Override public Optional<SocialCounter> findByUserId(UUID userId) { return Optional.ofNullable(rows.get(userId)); }
    }

    private static class RecordingSocialEventPublisher implements SocialEventPublisher {
        final List<FollowAcceptedEvent> followAcceptedEvents = new ArrayList<>();
        final List<FollowRejectedEvent> followRejectedEvents = new ArrayList<>();
        final List<FollowRemovedEvent> followRemovedEvents = new ArrayList<>();
        final List<FollowerRemovedEvent> followerRemovedEvents = new ArrayList<>();
        final List<UserBlockedEvent> userBlockedEvents = new ArrayList<>();
        final List<UserUnblockedEvent> userUnblockedEvents = new ArrayList<>();
        @Override public void publishFollowRequested(FollowRequestedEvent event) {}
        @Override public void publishFollowAccepted(FollowAcceptedEvent event) { followAcceptedEvents.add(event); }
        @Override public void publishFollowRejected(FollowRejectedEvent event) { followRejectedEvents.add(event); }
        @Override public void publishFollowRemoved(FollowRemovedEvent event) { followRemovedEvents.add(event); }
        @Override public void publishFollowerRemoved(FollowerRemovedEvent event) { followerRemovedEvents.add(event); }
        @Override public void publishUserBlocked(UserBlockedEvent event) { userBlockedEvents.add(event); }
        @Override public void publishUserUnblocked(UserUnblockedEvent event) { userUnblockedEvents.add(event); }
    }

    private static class RecordingSocialAuditLogger implements SocialAuditLogger {
        @Override public void log(SocialAuditEvent event) {}
    }
}
