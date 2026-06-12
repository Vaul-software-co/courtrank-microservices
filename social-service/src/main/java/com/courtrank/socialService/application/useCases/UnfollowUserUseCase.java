package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.application.dto.UnfollowUserRequest;
import com.courtrank.socialService.application.events.FollowRemovedEvent;
import com.courtrank.socialService.application.ports.SocialEventPublisher;
import com.courtrank.socialService.application.ports.audit.SocialAuditEventType;
import com.courtrank.socialService.application.ports.audit.SocialAuditLogger;
import com.courtrank.socialService.domain.entity.Follow;
import com.courtrank.socialService.domain.entity.SocialCounter;
import com.courtrank.socialService.domain.exceptions.FollowNotFoundException;
import com.courtrank.socialService.domain.repository.FollowRepository;
import com.courtrank.socialService.domain.repository.SocialCounterRepository;

import java.time.Instant;
import java.util.Map;

public class UnfollowUserUseCase extends SocialUseCaseSupport {
    private final FollowRepository followRepository;
    private final SocialCounterRepository socialCounterRepository;
    private final SocialEventPublisher eventPublisher;
    private final SocialAuditLogger auditLogger;

    public UnfollowUserUseCase(
            FollowRepository followRepository,
            SocialCounterRepository socialCounterRepository,
            SocialEventPublisher eventPublisher,
            SocialAuditLogger auditLogger
    ) {
        this.followRepository = followRepository;
        this.socialCounterRepository = socialCounterRepository;
        this.eventPublisher = eventPublisher;
        this.auditLogger = auditLogger;
    }

    public void execute(UnfollowUserRequest request, TraceContext trace) {
        Follow follow = this.followRepository.findByFollowerIdAndFollowingId(request.followerId(), request.followingId()).orElse(null);
        if (follow == null || !(follow.canBeUnfollowedBy(request.followerId()) || follow.canBeCanceledBy(request.followerId()))) {
            this.log(this.auditLogger, SocialAuditEventType.FOLLOW_REMOVE_FAILED_NOT_FOUND, request.followerId(), request.followingId(), trace, Map.of("reason", "follow_not_found"));
            throw new FollowNotFoundException();
        }

        this.followRepository.delete(follow);
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

        this.eventPublisher.publishFollowRemoved(new FollowRemovedEvent(follow.getId(), follow.getFollowerId(), follow.getFollowingId(), request.followerId(), Instant.now()));
        this.log(this.auditLogger, SocialAuditEventType.FOLLOW_REMOVED, request.followerId(), request.followingId(), trace, Map.of("followId", follow.getId().toString(), "status", follow.getFollowStatus().name()));
    }
}
