package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.AcceptFollowRequestRequest;
import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.application.events.FollowAcceptedEvent;
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

public class AcceptFollowRequestUseCase extends SocialUseCaseSupport {
    private final FollowRepository followRepository;
    private final SocialCounterRepository socialCounterRepository;
    private final SocialEventPublisher eventPublisher;
    private final SocialAuditLogger auditLogger;

    public AcceptFollowRequestUseCase(
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

    public void execute(AcceptFollowRequestRequest request, TraceContext trace) {
        Follow follow = this.followRepository.findById(request.followId()).orElse(null);
        if (follow == null || !follow.isPending() || !follow.isOwnedByFollowing(request.ownerId())) {
            this.log(
                    this.auditLogger,
                    SocialAuditEventType.FOLLOW_ACCEPT_FAILED_NOT_FOUND,
                    request.ownerId(),
                    request.followId(),
                    trace,
                    Map.of("reason", "pending_request_not_found")
            );
            throw new FollowNotFoundException();
        }

        follow.accept(request.ownerId());
        Follow savedFollow = this.followRepository.save(follow);

        SocialCounter followerCounter = this.findOrCreateCounter(this.socialCounterRepository, savedFollow.getFollowerId());
        SocialCounter followingCounter = this.findOrCreateCounter(this.socialCounterRepository, savedFollow.getFollowingId());
        followerCounter.acceptPendingRequestAsFollower();
        followingCounter.acceptPendingRequestAsFollowing();
        this.socialCounterRepository.save(followerCounter);
        this.socialCounterRepository.save(followingCounter);

        this.eventPublisher.publishFollowAccepted(new FollowAcceptedEvent(
                savedFollow.getId(),
                savedFollow.getFollowerId(),
                savedFollow.getFollowingId(),
                Instant.now()
        ));
        this.log(
                this.auditLogger,
                SocialAuditEventType.FOLLOW_ACCEPTED,
                request.ownerId(),
                savedFollow.getFollowerId(),
                trace,
                Map.of("followId", savedFollow.getId().toString())
        );
    }
}
