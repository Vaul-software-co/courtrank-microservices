package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.RejectFollowRequestRequest;
import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.application.events.FollowRejectedEvent;
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

public class RejectFollowRequestUseCase extends SocialUseCaseSupport {
    private final FollowRepository followRepository;
    private final SocialCounterRepository socialCounterRepository;
    private final SocialEventPublisher eventPublisher;
    private final SocialAuditLogger auditLogger;

    public RejectFollowRequestUseCase(
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

    public void execute(RejectFollowRequestRequest request, TraceContext trace) {
        Follow follow = this.followRepository.findById(request.followId()).orElse(null);
        if (follow == null || !follow.isPending() || !follow.isOwnedByFollowing(request.ownerId())) {
            this.log(this.auditLogger, SocialAuditEventType.FOLLOW_REJECT_FAILED_NOT_FOUND, request.ownerId(), request.followId(), trace, Map.of("reason", "pending_request_not_found"));
            throw new FollowNotFoundException();
        }

        this.followRepository.delete(follow);
        SocialCounter followingCounter = this.findOrCreateCounter(this.socialCounterRepository, follow.getFollowingId());
        followingCounter.removePendingRequestAsFollowing();
        this.socialCounterRepository.save(followingCounter);

        this.eventPublisher.publishFollowRejected(new FollowRejectedEvent(follow.getId(), follow.getFollowerId(), follow.getFollowingId(), Instant.now()));
        this.log(this.auditLogger, SocialAuditEventType.FOLLOW_REJECTED, request.ownerId(), follow.getFollowerId(), trace, Map.of("followId", follow.getId().toString()));
    }
}
