package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.RemoveFollowerRequest;
import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.application.events.FollowerRemovedEvent;
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

public class RemoveFollowerUseCase extends SocialUseCaseSupport {
    private final FollowRepository followRepository;
    private final SocialCounterRepository socialCounterRepository;
    private final SocialEventPublisher eventPublisher;
    private final SocialAuditLogger auditLogger;

    public RemoveFollowerUseCase(FollowRepository followRepository, SocialCounterRepository socialCounterRepository, SocialEventPublisher eventPublisher, SocialAuditLogger auditLogger) {
        this.followRepository = followRepository;
        this.socialCounterRepository = socialCounterRepository;
        this.eventPublisher = eventPublisher;
        this.auditLogger = auditLogger;
    }

    public void execute(RemoveFollowerRequest request, TraceContext trace) {
        Follow follow = this.followRepository.findByFollowerIdAndFollowingId(request.followerId(), request.ownerId()).orElse(null);
        if (follow == null || !follow.canBeRemovedBy(request.ownerId())) {
            this.log(this.auditLogger, SocialAuditEventType.FOLLOWER_REMOVE_FAILED_NOT_FOUND, request.ownerId(), request.followerId(), trace, Map.of("reason", "accepted_follower_not_found"));
            throw new FollowNotFoundException();
        }

        this.followRepository.delete(follow);
        SocialCounter followerCounter = this.findOrCreateCounter(this.socialCounterRepository, follow.getFollowerId());
        SocialCounter followingCounter = this.findOrCreateCounter(this.socialCounterRepository, follow.getFollowingId());
        followerCounter.removeAcceptedFollowAsFollower();
        followingCounter.removeAcceptedFollowAsFollowing();
        this.socialCounterRepository.save(followerCounter);
        this.socialCounterRepository.save(followingCounter);

        this.eventPublisher.publishFollowerRemoved(new FollowerRemovedEvent(follow.getId(), follow.getFollowerId(), follow.getFollowingId(), request.ownerId(), Instant.now()));
        this.log(this.auditLogger, SocialAuditEventType.FOLLOWER_REMOVED, request.ownerId(), request.followerId(), trace, Map.of("followId", follow.getId().toString()));
    }
}
