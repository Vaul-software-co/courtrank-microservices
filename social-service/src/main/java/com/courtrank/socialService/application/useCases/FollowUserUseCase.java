package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.FollowUserResponse;
import com.courtrank.socialService.application.dto.FollowUserRequest;
import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.application.events.FollowAcceptedEvent;
import com.courtrank.socialService.application.events.FollowRequestedEvent;
import com.courtrank.socialService.application.ports.SocialEventPublisher;
import com.courtrank.socialService.application.ports.audit.SocialAuditEvent;
import com.courtrank.socialService.application.ports.audit.SocialAuditEventType;
import com.courtrank.socialService.application.ports.audit.SocialAuditLogger;
import com.courtrank.socialService.domain.entity.Follow;
import com.courtrank.socialService.domain.entity.SocialCounter;
import com.courtrank.socialService.domain.entity.SocialUser;
import com.courtrank.socialService.domain.exceptions.SocialInteractionBlockedException;
import com.courtrank.socialService.domain.exceptions.SocialUserNotFoundException;
import com.courtrank.socialService.domain.repository.BlockRepository;
import com.courtrank.socialService.domain.repository.FollowRepository;
import com.courtrank.socialService.domain.repository.SocialCounterRepository;
import com.courtrank.socialService.domain.repository.SocialUserRepository;

import java.time.Instant;
import java.util.Map;

public class FollowUserUseCase extends SocialUseCaseSupport {

    private final FollowRepository followRepository;
    private final SocialUserRepository socialUserRepository;
    private final BlockRepository blockRepository;
    private final SocialCounterRepository socialCounterRepository;
    private final SocialEventPublisher eventPublisher;
    private final SocialAuditLogger auditLogger;

    public FollowUserUseCase(
            FollowRepository followRepository,
            SocialUserRepository socialUserRepository,
            BlockRepository blockRepository,
            SocialCounterRepository socialCounterRepository,
            SocialEventPublisher eventPublisher,
            SocialAuditLogger auditLogger
    ) {
        this.followRepository = followRepository;
        this.socialUserRepository = socialUserRepository;
        this.blockRepository = blockRepository;
        this.socialCounterRepository = socialCounterRepository;
        this.eventPublisher = eventPublisher;
        this.auditLogger = auditLogger;
    }

    public FollowUserResponse execute(FollowUserRequest request, TraceContext trace){
        SocialUser follower = this.socialUserRepository.findByUserId(request.followerId())
                .orElse(null);
        if (follower == null) {
            this.log(
                    this.auditLogger,
                    SocialAuditEventType.FOLLOW_CREATE_FAILED_FOLLOWER_NOT_FOUND,
                    request.followerId(),
                    request.followingId(),
                    trace,
                    Map.of("reason", "follower_not_found")
            );
            throw new SocialUserNotFoundException();
        }
        if (!follower.canBeShown()) {
            this.log(
                    this.auditLogger,
                    SocialAuditEventType.FOLLOW_CREATE_FAILED_FOLLOWER_NOT_FOUND,
                    request.followerId(),
                    request.followingId(),
                    trace,
                    Map.of("reason", "follower_not_visible")
            );
            throw new SocialUserNotFoundException();
        }

        SocialUser user = this.socialUserRepository.findByUserId(request.followingId())
                            .orElse(null);
        if (user == null) {
            this.log(
                    this.auditLogger,
                    SocialAuditEventType.FOLLOW_CREATE_FAILED_TARGET_NOT_FOUND,
                    request.followerId(),
                    request.followingId(),
                    trace,
                    Map.of("reason", "target_not_found")
            );
            throw new SocialUserNotFoundException();
        }
        if (!user.canBeShown()) {
            this.log(
                    this.auditLogger,
                    SocialAuditEventType.FOLLOW_CREATE_FAILED_TARGET_NOT_FOUND,
                    request.followerId(),
                    request.followingId(),
                    trace,
                    Map.of("reason", "target_not_visible")
            );
            throw new SocialUserNotFoundException();
        }

        Follow existingFollow = this.followRepository
                .findByFollowerIdAndFollowingId(request.followerId(), request.followingId())
                .orElse(null);

        if(existingFollow != null){
            this.log(
                    this.auditLogger,
                    SocialAuditEventType.FOLLOW_CREATE_SKIPPED_ALREADY_EXISTS,
                    request.followerId(),
                    request.followingId(),
                    trace,
                    Map.of(
                            "followId", existingFollow.getId().toString(),
                            "status", existingFollow.getFollowStatus().name()
                    )
            );
            return new FollowUserResponse(existingFollow.getId(), existingFollow.getFollowStatus());
        }

        if (this.blockRepository.existsBetweenUsers(request.followerId(), request.followingId())) {
            this.log(
                    this.auditLogger,
                    SocialAuditEventType.FOLLOW_CREATE_FAILED_BLOCKED,
                    request.followerId(),
                    request.followingId(),
                    trace,
                    Map.of("reason", "users_blocked")
            );
            throw new SocialInteractionBlockedException();
        }

        Follow follow = Follow.startFollowing(request.followerId(), user);
        Follow savedFollow = this.followRepository.save(follow);

        SocialCounter followerCounter = this.findOrCreateCounter(this.socialCounterRepository, request.followerId());
        SocialCounter followingCounter = this.findOrCreateCounter(this.socialCounterRepository, request.followingId());

        if (savedFollow.isAccepted()) {
            followerCounter.applyAcceptedFollowAsFollower();
            followingCounter.applyAcceptedFollowAsFollowing();
        } else {
            followingCounter.applyPendingRequestAsFollowing();
        }

        this.socialCounterRepository.save(followerCounter);
        this.socialCounterRepository.save(followingCounter);

        if (savedFollow.isAccepted()) {
            this.eventPublisher.publishFollowAccepted(new FollowAcceptedEvent(
                    savedFollow.getId(),
                    savedFollow.getFollowerId(),
                    savedFollow.getFollowingId(),
                    Instant.now()
            ));
            this.log(
                    this.auditLogger,
                    SocialAuditEventType.FOLLOW_CREATED_ACCEPTED,
                    request.followerId(),
                    request.followingId(),
                    trace,
                    Map.of("followId", savedFollow.getId().toString())
            );
        } else {
            this.eventPublisher.publishFollowRequested(new FollowRequestedEvent(
                    savedFollow.getId(),
                    savedFollow.getFollowerId(),
                    savedFollow.getFollowingId(),
                    Instant.now()
            ));
            this.log(
                    this.auditLogger,
                    SocialAuditEventType.FOLLOW_CREATED_PENDING,
                    request.followerId(),
                    request.followingId(),
                    trace,
                    Map.of("followId", savedFollow.getId().toString())
            );
        }

        return new FollowUserResponse(savedFollow.getId(), savedFollow.getFollowStatus());
    }

}
