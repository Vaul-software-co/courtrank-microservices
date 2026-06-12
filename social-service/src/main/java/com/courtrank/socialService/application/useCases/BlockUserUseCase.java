package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.BlockUserRequest;
import com.courtrank.socialService.application.dto.BlockUserResponse;
import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.application.events.UserBlockedEvent;
import com.courtrank.socialService.application.ports.SocialEventPublisher;
import com.courtrank.socialService.application.ports.audit.SocialAuditEventType;
import com.courtrank.socialService.application.ports.audit.SocialAuditLogger;
import com.courtrank.socialService.domain.entity.Block;
import com.courtrank.socialService.domain.entity.Follow;
import com.courtrank.socialService.domain.entity.SocialCounter;
import com.courtrank.socialService.domain.entity.SocialUser;
import com.courtrank.socialService.domain.exceptions.SocialUserNotFoundException;
import com.courtrank.socialService.domain.repository.BlockRepository;
import com.courtrank.socialService.domain.repository.FollowRepository;
import com.courtrank.socialService.domain.repository.SocialCounterRepository;
import com.courtrank.socialService.domain.repository.SocialUserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class BlockUserUseCase extends SocialUseCaseSupport {
    private final BlockRepository blockRepository;
    private final FollowRepository followRepository;
    private final SocialUserRepository socialUserRepository;
    private final SocialCounterRepository socialCounterRepository;
    private final SocialEventPublisher eventPublisher;
    private final SocialAuditLogger auditLogger;

    public BlockUserUseCase(
            BlockRepository blockRepository,
            FollowRepository followRepository,
            SocialUserRepository socialUserRepository,
            SocialCounterRepository socialCounterRepository,
            SocialEventPublisher eventPublisher,
            SocialAuditLogger auditLogger
    ) {
        this.blockRepository = blockRepository;
        this.followRepository = followRepository;
        this.socialUserRepository = socialUserRepository;
        this.socialCounterRepository = socialCounterRepository;
        this.eventPublisher = eventPublisher;
        this.auditLogger = auditLogger;
    }

    public BlockUserResponse execute(BlockUserRequest request, TraceContext trace) {
        SocialUser blocker = this.socialUserRepository.findByUserId(request.blockerId()).orElse(null);
        if (blocker == null || !blocker.canBeShown()) {
            this.log(this.auditLogger, SocialAuditEventType.BLOCK_CREATE_FAILED_BLOCKER_NOT_FOUND, request.blockerId(), request.blockedId(), trace, Map.of("reason", "blocker_not_found"));
            throw new SocialUserNotFoundException();
        }

        SocialUser blocked = this.socialUserRepository.findByUserId(request.blockedId()).orElse(null);
        if (blocked == null || !blocked.canBeShown()) {
            this.log(this.auditLogger, SocialAuditEventType.BLOCK_CREATE_FAILED_TARGET_NOT_FOUND, request.blockerId(), request.blockedId(), trace, Map.of("reason", "target_not_found"));
            throw new SocialUserNotFoundException();
        }

        Block existingBlock = this.blockRepository.findByBlockerIdAndBlockedId(request.blockerId(), request.blockedId()).orElse(null);
        if (existingBlock != null) {
            this.log(this.auditLogger, SocialAuditEventType.BLOCK_CREATE_SKIPPED_ALREADY_EXISTS, request.blockerId(), request.blockedId(), trace, Map.of("blockId", existingBlock.getId().toString()));
            return new BlockUserResponse(existingBlock.getId(), false);
        }

        Block block = this.blockRepository.save(Block.blockUser(request.blockerId(), blocked));
        List<Follow> affectedFollows = this.followRepository.findBetweenUsers(request.blockerId(), request.blockedId());
        for (Follow follow : affectedFollows) {
            removeFollowFromCounters(follow);
            this.followRepository.delete(follow);
        }

        SocialCounter blockerCounter = this.findOrCreateCounter(this.socialCounterRepository, request.blockerId());
        blockerCounter.applyBlockAsBlocker();
        this.socialCounterRepository.save(blockerCounter);

        this.eventPublisher.publishUserBlocked(new UserBlockedEvent(block.getId(), block.getBlockerId(), block.getBlockedId(), Instant.now()));
        this.log(this.auditLogger, SocialAuditEventType.BLOCK_CREATED, request.blockerId(), request.blockedId(), trace, Map.of("blockId", block.getId().toString(), "removedFollows", affectedFollows.size()));
        return new BlockUserResponse(block.getId(), true);
    }

    private void removeFollowFromCounters(Follow follow) {
        if (follow.isAccepted()) {
            SocialCounter followerCounter = this.findOrCreateCounter(this.socialCounterRepository, follow.getFollowerId());
            SocialCounter followingCounter = this.findOrCreateCounter(this.socialCounterRepository, follow.getFollowingId());
            followerCounter.removeAcceptedFollowAsFollower();
            followingCounter.removeAcceptedFollowAsFollowing();
            this.socialCounterRepository.save(followerCounter);
            this.socialCounterRepository.save(followingCounter);
        } else {
            SocialCounter followingCounter = this.findOrCreateCounter(this.socialCounterRepository, follow.getFollowingId());
            followingCounter.removePendingRequestAsFollowing();
            this.socialCounterRepository.save(followingCounter);
        }
    }
}
