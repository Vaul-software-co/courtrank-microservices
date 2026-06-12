package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.events.FollowAcceptedEvent;
import com.courtrank.socialService.application.ports.SocialEventPublisher;
import com.courtrank.socialService.domain.entity.Follow;
import com.courtrank.socialService.domain.entity.SocialCounter;
import com.courtrank.socialService.domain.repository.FollowRepository;
import com.courtrank.socialService.domain.repository.SocialCounterRepository;

import java.time.Instant;
import java.util.UUID;

public class HandleUserBecamePublicUseCase extends SocialUseCaseSupport {
    private final FollowRepository followRepository;
    private final SocialCounterRepository socialCounterRepository;
    private final SocialEventPublisher eventPublisher;

    public HandleUserBecamePublicUseCase(FollowRepository followRepository, SocialCounterRepository socialCounterRepository, SocialEventPublisher eventPublisher) {
        this.followRepository = followRepository;
        this.socialCounterRepository = socialCounterRepository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(UUID userId) {
        for (Follow follow : this.followRepository.findPendingByFollowingId(userId)) {
            follow.accept(userId);
            Follow saved = this.followRepository.save(follow);
            SocialCounter followerCounter = this.findOrCreateCounter(this.socialCounterRepository, saved.getFollowerId());
            SocialCounter followingCounter = this.findOrCreateCounter(this.socialCounterRepository, saved.getFollowingId());
            followerCounter.acceptPendingRequestAsFollower();
            followingCounter.acceptPendingRequestAsFollowing();
            this.socialCounterRepository.save(followerCounter);
            this.socialCounterRepository.save(followingCounter);
            this.eventPublisher.publishFollowAccepted(new FollowAcceptedEvent(saved.getId(), saved.getFollowerId(), saved.getFollowingId(), Instant.now()));
        }
    }
}
