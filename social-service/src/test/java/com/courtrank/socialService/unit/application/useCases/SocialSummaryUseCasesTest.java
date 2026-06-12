package com.courtrank.socialService.unit.application.useCases;

import com.courtrank.socialService.application.dto.AreUsersBlockedRequest;
import com.courtrank.socialService.application.dto.GetFollowStatusRequest;
import com.courtrank.socialService.application.dto.GetRelatedBlockedUserIdsRequest;
import com.courtrank.socialService.application.dto.GetSocialCountersRequest;
import com.courtrank.socialService.application.dto.GetUserSocialSummaryRequest;
import com.courtrank.socialService.application.dto.SearchSocialUsersRequest;
import com.courtrank.socialService.application.dto.SocialCountersResponse;
import com.courtrank.socialService.application.dto.SocialUserSummary;
import com.courtrank.socialService.application.dto.UserSocialSummaryResponse;
import com.courtrank.socialService.application.useCases.AreUsersBlockedUseCase;
import com.courtrank.socialService.application.useCases.GetFollowStatusUseCase;
import com.courtrank.socialService.application.useCases.GetRelatedBlockedUserIdsUseCase;
import com.courtrank.socialService.application.useCases.GetSocialCountersUseCase;
import com.courtrank.socialService.application.useCases.GetUserSocialSummaryUseCase;
import com.courtrank.socialService.application.useCases.SearchSocialUsersUseCase;
import com.courtrank.socialService.domain.entity.Block;
import com.courtrank.socialService.domain.entity.Follow;
import com.courtrank.socialService.domain.entity.SocialCounter;
import com.courtrank.socialService.domain.entity.SocialUser;
import com.courtrank.socialService.domain.enums.ViewerFollowStatus;
import com.courtrank.socialService.domain.repository.BlockRepository;
import com.courtrank.socialService.domain.repository.FollowRepository;
import com.courtrank.socialService.domain.repository.SocialCounterRepository;
import com.courtrank.socialService.domain.repository.SocialUserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SocialSummaryUseCasesTest {
    @Test
    void summaryUseCases_shouldReturnStatusCountersAndSearchResults() {
        Fixture fx = new Fixture();
        UUID viewerId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        fx.users.save(user(viewerId, "Viewer"));
        fx.users.save(user(targetId, "Ana Target"));
        fx.users.save(user(blockedId, "Ana Blocked"));
        fx.follows.save(Follow.startFollowing(viewerId, user(targetId, "Ana Target")));
        SocialCounter targetCounter = SocialCounter.create(targetId);
        targetCounter.applyAcceptedFollowAsFollowing();
        fx.counters.save(targetCounter);
        fx.blocks.save(Block.blockUser(viewerId, user(blockedId, "Ana Blocked")));

        ViewerFollowStatus status = new GetFollowStatusUseCase(fx.follows)
                .execute(new GetFollowStatusRequest(viewerId, targetId), null);
        SocialCountersResponse counters = new GetSocialCountersUseCase(fx.counters)
                .execute(new GetSocialCountersRequest(targetId), null);
        UserSocialSummaryResponse summary = new GetUserSocialSummaryUseCase(fx.users, fx.follows, fx.blocks, fx.counters)
                .execute(new GetUserSocialSummaryRequest(viewerId, targetId), null);
        List<SocialUserSummary> search = new SearchSocialUsersUseCase(fx.users, fx.blocks)
                .execute(new SearchSocialUsersRequest(viewerId, "ana", 20), null);
        boolean blocked = new AreUsersBlockedUseCase(fx.blocks)
                .execute(new AreUsersBlockedRequest(viewerId, blockedId), null);
        Set<UUID> relatedBlocked = new GetRelatedBlockedUserIdsUseCase(fx.blocks)
                .execute(new GetRelatedBlockedUserIdsRequest(viewerId), null);

        assertThat(status).isEqualTo(ViewerFollowStatus.ACCEPTED);
        assertThat(counters.followersCount()).isEqualTo(1);
        assertThat(summary.viewerFollowStatus()).isEqualTo(ViewerFollowStatus.ACCEPTED);
        assertThat(summary.followersCount()).isEqualTo(1);
        assertThat(search).extracting(SocialUserSummary::userId).containsExactly(targetId);
        assertThat(blocked).isTrue();
        assertThat(relatedBlocked).containsExactly(blockedId);
    }

    private static SocialUser user(UUID id, String name) {
        return SocialUser.create(id, name, name.toLowerCase().replace(" ", "_"), null, false, true, Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static String followKey(UUID followerId, UUID followingId) {
        return followerId + ":" + followingId;
    }

    private static class Fixture {
        final InMemoryFollowRepository follows = new InMemoryFollowRepository();
        final InMemoryBlockRepository blocks = new InMemoryBlockRepository();
        final InMemorySocialUserRepository users = new InMemorySocialUserRepository();
        final InMemorySocialCounterRepository counters = new InMemorySocialCounterRepository();
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
        @Override public List<Block> findByBlockerId(UUID blockerId) { return rows.values().stream().filter(b -> b.getBlockerId().equals(blockerId)).toList(); }
        @Override public boolean existsBetweenUsers(UUID userA, UUID userB) { return rows.values().stream().anyMatch(b -> b.isBetween(userA, userB)); }
        @Override public Set<UUID> findRelatedUserIds(UUID userId) { return rows.values().stream().filter(b -> b.involves(userId)).map(b -> b.getBlockerId().equals(userId) ? b.getBlockedId() : b.getBlockerId()).collect(Collectors.toSet()); }
        @Override public void delete(Block block) { rows.remove(followKey(block.getBlockerId(), block.getBlockedId())); }
    }

    private static class InMemorySocialUserRepository implements SocialUserRepository {
        private final Map<UUID, SocialUser> rows = new HashMap<>();
        @Override public void save(SocialUser socialUser) { rows.put(socialUser.getUserId(), socialUser); }
        @Override public Optional<SocialUser> findByUserId(UUID userId) { return Optional.ofNullable(rows.get(userId)); }
        @Override public List<UUID> findAllUserIds() { return List.copyOf(rows.keySet()); }
        @Override public List<SocialUser> searchVisible(String query, int limit, Set<UUID> excludedUserIds) {
            return rows.values().stream()
                    .filter(SocialUser::canBeShown)
                    .filter(user -> !excludedUserIds.contains(user.getUserId()))
                    .filter(user -> user.getName().toLowerCase().contains(query.toLowerCase()) || user.getUsername().toLowerCase().contains(query.toLowerCase()))
                    .limit(limit)
                    .toList();
        }
    }

    private static class InMemorySocialCounterRepository implements SocialCounterRepository {
        private final Map<UUID, SocialCounter> rows = new HashMap<>();
        @Override public SocialCounter save(SocialCounter socialCounter) { rows.put(socialCounter.getUserId(), socialCounter); return socialCounter; }
        @Override public Optional<SocialCounter> findByUserId(UUID userId) { return Optional.ofNullable(rows.get(userId)); }
    }
}
