package com.courtrank.socialService.unit.application.useCases;

import com.courtrank.socialService.application.dto.BlockedUserSummary;
import com.courtrank.socialService.application.dto.FollowRequestSummary;
import com.courtrank.socialService.application.dto.ListBlockedUsersRequest;
import com.courtrank.socialService.application.dto.ListFollowRequestsRequest;
import com.courtrank.socialService.application.dto.ListFollowersRequest;
import com.courtrank.socialService.application.dto.ListFollowingRequest;
import com.courtrank.socialService.application.dto.SocialUserSummary;
import com.courtrank.socialService.application.useCases.ListBlockedUsersUseCase;
import com.courtrank.socialService.application.useCases.ListFollowersUseCase;
import com.courtrank.socialService.application.useCases.ListFollowingUseCase;
import com.courtrank.socialService.application.useCases.ListMyFollowRequestsUseCase;
import com.courtrank.socialService.domain.entity.Block;
import com.courtrank.socialService.domain.entity.Follow;
import com.courtrank.socialService.domain.entity.SocialUser;
import com.courtrank.socialService.domain.repository.BlockRepository;
import com.courtrank.socialService.domain.repository.FollowRepository;
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

class SocialListUseCasesTest {
    @Test
    void listMyFollowRequests_shouldReturnPendingRequestsWithFollowerSummary() {
        Fixture fx = new Fixture();
        UUID followerId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        fx.users.save(user(followerId, false, "Follower"));
        fx.users.save(user(ownerId, true, "Owner"));
        Follow request = fx.follows.save(Follow.startFollowing(followerId, user(ownerId, true, "Owner")));

        List<FollowRequestSummary> result = new ListMyFollowRequestsUseCase(fx.follows, fx.users)
                .execute(new ListFollowRequestsRequest(ownerId), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).followId()).isEqualTo(request.getId());
        assertThat(result.get(0).user().userId()).isEqualTo(followerId);
    }

    @Test
    void listFollowers_shouldRespectPrivateVisibility() {
        Fixture fx = new Fixture();
        UUID viewerId = UUID.randomUUID();
        UUID followerId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        fx.users.save(user(viewerId, false, "Viewer"));
        fx.users.save(user(followerId, false, "Follower"));
        fx.users.save(user(targetId, true, "Private"));
        fx.follows.save(Follow.startFollowing(followerId, user(targetId, false, "Private")));

        List<SocialUserSummary> hidden = new ListFollowersUseCase(fx.follows, fx.users, fx.blocks)
                .execute(new ListFollowersRequest(viewerId, targetId), null);
        List<SocialUserSummary> visibleToOwner = new ListFollowersUseCase(fx.follows, fx.users, fx.blocks)
                .execute(new ListFollowersRequest(targetId, targetId), null);

        assertThat(hidden).isEmpty();
        assertThat(visibleToOwner).extracting(SocialUserSummary::userId).containsExactly(followerId);
    }

    @Test
    void listFollowing_shouldReturnAcceptedFollowingForVisibleProfile() {
        Fixture fx = new Fixture();
        UUID viewerId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();
        fx.users.save(user(viewerId, false, "Viewer"));
        fx.users.save(user(targetId, false, "Target"));
        fx.users.save(user(followingId, false, "Following"));
        fx.follows.save(Follow.startFollowing(targetId, user(followingId, false, "Following")));

        List<SocialUserSummary> result = new ListFollowingUseCase(fx.follows, fx.users, fx.blocks)
                .execute(new ListFollowingRequest(viewerId, targetId), null);

        assertThat(result).extracting(SocialUserSummary::userId).containsExactly(followingId);
    }

    @Test
    void listBlockedUsers_shouldReturnUsersBlockedByOwner() {
        Fixture fx = new Fixture();
        UUID blockerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        fx.users.save(user(blockedId, false, "Blocked"));
        fx.blocks.save(Block.blockUser(blockerId, user(blockedId, false, "Blocked")));

        List<BlockedUserSummary> result = new ListBlockedUsersUseCase(fx.blocks, fx.users)
                .execute(new ListBlockedUsersRequest(blockerId), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).user().userId()).isEqualTo(blockedId);
    }

    private static SocialUser user(UUID id, boolean isPrivate, String name) {
        return SocialUser.create(id, name, name.toLowerCase(), null, isPrivate, true, Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static String followKey(UUID followerId, UUID followingId) {
        return followerId + ":" + followingId;
    }

    private static class Fixture {
        final InMemoryFollowRepository follows = new InMemoryFollowRepository();
        final InMemoryBlockRepository blocks = new InMemoryBlockRepository();
        final InMemorySocialUserRepository users = new InMemorySocialUserRepository();
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
        private final List<Block> rows = new ArrayList<>();
        @Override public Block save(Block block) { rows.add(block); return block; }
        @Override public Optional<Block> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId) { return rows.stream().filter(b -> b.getBlockerId().equals(blockerId) && b.getBlockedId().equals(blockedId)).findFirst(); }
        @Override public List<Block> findByBlockerId(UUID blockerId) { return rows.stream().filter(b -> b.getBlockerId().equals(blockerId)).toList(); }
        @Override public boolean existsBetweenUsers(UUID userA, UUID userB) { return rows.stream().anyMatch(b -> b.isBetween(userA, userB)); }
        @Override public java.util.Set<UUID> findRelatedUserIds(UUID userId) { return rows.stream().filter(b -> b.involves(userId)).map(b -> b.getBlockerId().equals(userId) ? b.getBlockedId() : b.getBlockerId()).collect(java.util.stream.Collectors.toSet()); }
        @Override public void delete(Block block) { rows.remove(block); }
    }

    private static class InMemorySocialUserRepository implements SocialUserRepository {
        private final Map<UUID, SocialUser> rows = new HashMap<>();
        @Override public void save(SocialUser socialUser) { rows.put(socialUser.getUserId(), socialUser); }
        @Override public Optional<SocialUser> findByUserId(UUID userId) { return Optional.ofNullable(rows.get(userId)); }
        @Override public List<UUID> findAllUserIds() { return List.copyOf(rows.keySet()); }
        @Override public List<SocialUser> searchVisible(String query, int limit, java.util.Set<UUID> excludedUserIds) { return rows.values().stream().filter(SocialUser::canBeShown).filter(user -> !excludedUserIds.contains(user.getUserId())).filter(user -> user.getName().toLowerCase().contains(query.toLowerCase()) || user.getUsername().toLowerCase().contains(query.toLowerCase())).limit(limit).toList(); }
    }
}
