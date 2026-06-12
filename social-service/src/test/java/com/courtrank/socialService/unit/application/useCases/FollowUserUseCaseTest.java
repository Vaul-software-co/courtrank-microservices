package com.courtrank.socialService.unit.application.useCases;

import com.courtrank.socialService.application.dto.FollowUserRequest;
import com.courtrank.socialService.application.dto.FollowUserResponse;
import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.application.events.FollowAcceptedEvent;
import com.courtrank.socialService.application.events.FollowRejectedEvent;
import com.courtrank.socialService.application.events.FollowRemovedEvent;
import com.courtrank.socialService.application.events.FollowRequestedEvent;
import com.courtrank.socialService.application.events.FollowerRemovedEvent;
import com.courtrank.socialService.application.events.UserBlockedEvent;
import com.courtrank.socialService.application.events.UserUnblockedEvent;
import com.courtrank.socialService.application.ports.SocialEventPublisher;
import com.courtrank.socialService.application.ports.audit.SocialAuditEvent;
import com.courtrank.socialService.application.ports.audit.SocialAuditEventType;
import com.courtrank.socialService.application.ports.audit.SocialAuditLogger;
import com.courtrank.socialService.application.useCases.FollowUserUseCase;
import com.courtrank.socialService.domain.entity.Block;
import com.courtrank.socialService.domain.entity.Follow;
import com.courtrank.socialService.domain.entity.SocialCounter;
import com.courtrank.socialService.domain.entity.SocialUser;
import com.courtrank.socialService.domain.enums.FollowStatus;
import com.courtrank.socialService.domain.exceptions.SocialInteractionBlockedException;
import com.courtrank.socialService.domain.exceptions.SocialUserNotFoundException;
import com.courtrank.socialService.domain.repository.BlockRepository;
import com.courtrank.socialService.domain.repository.FollowRepository;
import com.courtrank.socialService.domain.repository.SocialCounterRepository;
import com.courtrank.socialService.domain.repository.SocialUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FollowUserUseCaseTest {
    private InMemoryFollowRepository followRepository;
    private InMemorySocialUserRepository socialUserRepository;
    private InMemoryBlockRepository blockRepository;
    private InMemorySocialCounterRepository socialCounterRepository;
    private RecordingSocialEventPublisher eventPublisher;
    private RecordingSocialAuditLogger auditLogger;
    private FollowUserUseCase useCase;

    @BeforeEach
    void setUp() {
        this.followRepository = new InMemoryFollowRepository();
        this.socialUserRepository = new InMemorySocialUserRepository();
        this.blockRepository = new InMemoryBlockRepository();
        this.socialCounterRepository = new InMemorySocialCounterRepository();
        this.eventPublisher = new RecordingSocialEventPublisher();
        this.auditLogger = new RecordingSocialAuditLogger();
        this.useCase = new FollowUserUseCase(
                this.followRepository,
                this.socialUserRepository,
                this.blockRepository,
                this.socialCounterRepository,
                this.eventPublisher,
                this.auditLogger
        );
    }

    @Test
    void execute_shouldCreateAcceptedFollowForPublicTarget() {
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();
        this.socialUserRepository.save(socialUser(followerId, false));
        this.socialUserRepository.save(socialUser(followingId, false));

        FollowUserResponse response = this.useCase.execute(
                new FollowUserRequest(followerId, followingId),
                TraceContext.fromRequestId("req-1")
        );

        assertThat(response.status()).isEqualTo(FollowStatus.ACCEPTED);
        assertThat(this.followRepository.findByFollowerIdAndFollowingId(followerId, followingId)).isPresent();
        assertThat(this.socialCounterRepository.findByUserId(followerId).orElseThrow().getFollowingCount()).isEqualTo(1);
        assertThat(this.socialCounterRepository.findByUserId(followingId).orElseThrow().getFollowersCount()).isEqualTo(1);
        assertThat(this.eventPublisher.followAcceptedEvents).hasSize(1);
        assertThat(this.eventPublisher.followRequestedEvents).isEmpty();
        assertThat(this.auditLogger.events).extracting(SocialAuditEvent::type)
                .containsExactly(SocialAuditEventType.FOLLOW_CREATED_ACCEPTED);
        assertThat(this.auditLogger.events.get(0).traceId()).isEqualTo("req-1");
    }

    @Test
    void execute_shouldCreatePendingFollowForPrivateTarget() {
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();
        this.socialUserRepository.save(socialUser(followerId, false));
        this.socialUserRepository.save(socialUser(followingId, true));

        FollowUserResponse response = this.useCase.execute(
                new FollowUserRequest(followerId, followingId),
                null
        );

        assertThat(response.status()).isEqualTo(FollowStatus.PENDING);
        assertThat(this.socialCounterRepository.findByUserId(followerId).orElseThrow().getFollowingCount()).isZero();
        assertThat(this.socialCounterRepository.findByUserId(followingId).orElseThrow().getFollowersCount()).isZero();
        assertThat(this.socialCounterRepository.findByUserId(followingId).orElseThrow().getPendingRequestsCount()).isEqualTo(1);
        assertThat(this.eventPublisher.followRequestedEvents).hasSize(1);
        assertThat(this.eventPublisher.followAcceptedEvents).isEmpty();
        assertThat(this.auditLogger.events).extracting(SocialAuditEvent::type)
                .containsExactly(SocialAuditEventType.FOLLOW_CREATED_PENDING);
    }

    @Test
    void execute_shouldReturnExistingFollowWithoutChangingCountersOrPublishingEvent() {
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();
        this.socialUserRepository.save(socialUser(followerId, false));
        this.socialUserRepository.save(socialUser(followingId, false));
        Follow existing = this.followRepository.save(Follow.startFollowing(followerId, socialUser(followingId, false)));

        FollowUserResponse response = this.useCase.execute(
                new FollowUserRequest(followerId, followingId),
                null
        );

        assertThat(response.followId()).isEqualTo(existing.getId());
        assertThat(response.status()).isEqualTo(FollowStatus.ACCEPTED);
        assertThat(this.socialCounterRepository.savedCounters).isEmpty();
        assertThat(this.eventPublisher.followAcceptedEvents).isEmpty();
        assertThat(this.eventPublisher.followRequestedEvents).isEmpty();
        assertThat(this.auditLogger.events).extracting(SocialAuditEvent::type)
                .containsExactly(SocialAuditEventType.FOLLOW_CREATE_SKIPPED_ALREADY_EXISTS);
    }

    @Test
    void execute_shouldRejectWhenTargetDoesNotExist() {
        UUID followerId = UUID.randomUUID();
        this.socialUserRepository.save(socialUser(followerId, false));

        assertThatThrownBy(() -> this.useCase.execute(
                new FollowUserRequest(followerId, UUID.randomUUID()),
                null
        )).isInstanceOf(SocialUserNotFoundException.class);
        assertThat(this.auditLogger.events).extracting(SocialAuditEvent::type)
                .containsExactly(SocialAuditEventType.FOLLOW_CREATE_FAILED_TARGET_NOT_FOUND);
    }

    @Test
    void execute_shouldRejectWhenFollowerDoesNotExist() {
        UUID followingId = UUID.randomUUID();
        this.socialUserRepository.save(socialUser(followingId, false));

        assertThatThrownBy(() -> this.useCase.execute(
                new FollowUserRequest(UUID.randomUUID(), followingId),
                null
        )).isInstanceOf(SocialUserNotFoundException.class);
        assertThat(this.auditLogger.events).extracting(SocialAuditEvent::type)
                .containsExactly(SocialAuditEventType.FOLLOW_CREATE_FAILED_FOLLOWER_NOT_FOUND);
    }

    @Test
    void execute_shouldRejectWhenUsersAreBlocked() {
        UUID followerId = UUID.randomUUID();
        UUID followingId = UUID.randomUUID();
        this.socialUserRepository.save(socialUser(followerId, false));
        this.socialUserRepository.save(socialUser(followingId, false));
        this.blockRepository.blockedPairs.add(pairKey(followerId, followingId));

        assertThatThrownBy(() -> this.useCase.execute(
                new FollowUserRequest(followerId, followingId),
                null
        )).isInstanceOf(SocialInteractionBlockedException.class);
        assertThat(this.auditLogger.events).extracting(SocialAuditEvent::type)
                .containsExactly(SocialAuditEventType.FOLLOW_CREATE_FAILED_BLOCKED);
    }

    private static SocialUser socialUser(UUID userId, boolean isPrivate) {
        return SocialUser.create(
                userId,
                "Target",
                "target",
                null,
                isPrivate,
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }

    private static String pairKey(UUID userA, UUID userB) {
        return userA.compareTo(userB) < 0
                ? userA + ":" + userB
                : userB + ":" + userA;
    }

    private static class InMemoryFollowRepository implements FollowRepository {
        private final Map<String, Follow> follows = new HashMap<>();

        @Override
        public Follow save(Follow follow) {
            this.follows.put(key(follow.getFollowerId(), follow.getFollowingId()), follow);
            return follow;
        }

        @Override
        public Optional<Follow> findById(UUID id) {
            return this.follows.values().stream().filter(follow -> follow.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId) {
            return Optional.ofNullable(this.follows.get(key(followerId, followingId)));
        }

        @Override
        public boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId) {
            return this.follows.containsKey(key(followerId, followingId));
        }

        @Override
        public boolean existsAcceptedByFollowerIdAndFollowingId(UUID followerId, UUID followingId) {
            return findByFollowerIdAndFollowingId(followerId, followingId)
                    .map(Follow::isAccepted)
                    .orElse(false);
        }

        @Override
        public List<Follow> findFollowersByFollowingId(UUID followingId) {
            return this.follows.values().stream()
                    .filter(follow -> follow.getFollowingId().equals(followingId))
                    .toList();
        }

        @Override
        public List<Follow> findFollowingByFollowerId(UUID followerId) {
            return this.follows.values().stream()
                    .filter(follow -> follow.getFollowerId().equals(followerId))
                    .toList();
        }

        @Override
        public List<Follow> findPendingByFollowingId(UUID followingId) {
            return this.follows.values().stream()
                    .filter(follow -> follow.getFollowingId().equals(followingId))
                    .filter(Follow::isPending)
                    .toList();
        }

        @Override
        public List<Follow> findBetweenUsers(UUID userA, UUID userB) {
            return this.follows.values().stream()
                    .filter(follow -> follow.isBetween(userA, userB))
                    .toList();
        }

        @Override
        public void delete(Follow follow) {
            this.follows.remove(key(follow.getFollowerId(), follow.getFollowingId()));
        }

        private String key(UUID followerId, UUID followingId) {
            return followerId + ":" + followingId;
        }
    }

    private static class InMemorySocialUserRepository implements SocialUserRepository {
        private final Map<UUID, SocialUser> users = new HashMap<>();

        @Override
        public void save(SocialUser socialUser) {
            this.users.put(socialUser.getUserId(), socialUser);
        }

        @Override
        public Optional<SocialUser> findByUserId(UUID userId) {
            return Optional.ofNullable(this.users.get(userId));
        }

        @Override
        public List<UUID> findAllUserIds() {
            return List.copyOf(this.users.keySet());
        }

        @Override
        public List<SocialUser> searchVisible(String query, int limit, java.util.Set<UUID> excludedUserIds) {
            return List.of();
        }
    }

    private static class InMemoryBlockRepository implements BlockRepository {
        private final List<String> blockedPairs = new ArrayList<>();

        @Override
        public Block save(Block block) {
            this.blockedPairs.add(pairKey(block.getBlockerId(), block.getBlockedId()));
            return block;
        }

        @Override
        public Optional<Block> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId) {
            return Optional.empty();
        }

        @Override
        public List<Block> findByBlockerId(UUID blockerId) {
            return List.of();
        }

        @Override
        public boolean existsBetweenUsers(UUID userA, UUID userB) {
            return this.blockedPairs.contains(pairKey(userA, userB));
        }

        @Override
        public java.util.Set<UUID> findRelatedUserIds(UUID userId) {
            return java.util.Set.of();
        }

        @Override
        public void delete(Block block) {
            this.blockedPairs.remove(pairKey(block.getBlockerId(), block.getBlockedId()));
        }
    }

    private static class InMemorySocialCounterRepository implements SocialCounterRepository {
        private final Map<UUID, SocialCounter> counters = new HashMap<>();
        private final List<SocialCounter> savedCounters = new ArrayList<>();

        @Override
        public SocialCounter save(SocialCounter socialCounter) {
            this.counters.put(socialCounter.getUserId(), socialCounter);
            this.savedCounters.add(socialCounter);
            return socialCounter;
        }

        @Override
        public Optional<SocialCounter> findByUserId(UUID userId) {
            return Optional.ofNullable(this.counters.get(userId));
        }
    }

    private static class RecordingSocialEventPublisher implements SocialEventPublisher {
        private final List<FollowRequestedEvent> followRequestedEvents = new ArrayList<>();
        private final List<FollowAcceptedEvent> followAcceptedEvents = new ArrayList<>();

        @Override
        public void publishFollowRequested(FollowRequestedEvent event) {
            this.followRequestedEvents.add(event);
        }

        @Override
        public void publishFollowAccepted(FollowAcceptedEvent event) {
            this.followAcceptedEvents.add(event);
        }

        @Override
        public void publishFollowRejected(FollowRejectedEvent event) {
        }

        @Override
        public void publishFollowRemoved(FollowRemovedEvent event) {
        }

        @Override
        public void publishFollowerRemoved(FollowerRemovedEvent event) {
        }

        @Override
        public void publishUserBlocked(UserBlockedEvent event) {
        }

        @Override
        public void publishUserUnblocked(UserUnblockedEvent event) {
        }
    }

    private static class RecordingSocialAuditLogger implements SocialAuditLogger {
        private final List<SocialAuditEvent> events = new ArrayList<>();

        @Override
        public void log(SocialAuditEvent event) {
            this.events.add(event);
        }
    }
}
