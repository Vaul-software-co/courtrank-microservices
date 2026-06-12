package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.SyncSocialUserRequest;
import com.courtrank.socialService.application.events.FollowAcceptedEvent;
import com.courtrank.socialService.application.ports.SocialEventPublisher;
import com.courtrank.socialService.domain.entity.Follow;
import com.courtrank.socialService.domain.entity.SocialCounter;
import com.courtrank.socialService.domain.entity.SocialUser;
import com.courtrank.socialService.domain.repository.FollowRepository;
import com.courtrank.socialService.domain.repository.SocialCounterRepository;
import com.courtrank.socialService.domain.repository.SocialUserRepository;

import java.time.Instant;

public class UpdateSocialUserFromUserEventUseCase extends SocialUseCaseSupport {
    private final SocialUserRepository socialUserRepository;
    private final SocialCounterRepository socialCounterRepository;
    private final FollowRepository followRepository;
    private final SocialEventPublisher eventPublisher;

    public UpdateSocialUserFromUserEventUseCase(
            SocialUserRepository socialUserRepository,
            SocialCounterRepository socialCounterRepository,
            FollowRepository followRepository,
            SocialEventPublisher eventPublisher
    ) {
        this.socialUserRepository = socialUserRepository;
        this.socialCounterRepository = socialCounterRepository;
        this.followRepository = followRepository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(SyncSocialUserRequest request) {
        var snapshot = request.snapshot();
        SocialUser user = this.socialUserRepository.findByUserId(snapshot.userId()).orElse(null);
        boolean wasPrivate = user != null && user.isPrivate();

        if (user == null) {
            user = SocialUser.create(snapshot.userId(), snapshot.name(), snapshot.username(), snapshot.avatarUrl(), snapshot.privateProfile(), snapshot.active(), snapshot.sourceUpdatedAt());
        } else {
            user.syncProfile(snapshot.name(), snapshot.username(), snapshot.avatarUrl(), snapshot.privateProfile(), snapshot.active(), snapshot.sourceUpdatedAt());
        }
        this.socialUserRepository.save(user);
        if (this.socialCounterRepository.findByUserId(snapshot.userId()).isEmpty()) {
            this.socialCounterRepository.save(SocialCounter.create(snapshot.userId()));
        }

        if (wasPrivate && !user.isPrivate()) {
            acceptPendingFollows(user);
        }
    }

    private void acceptPendingFollows(SocialUser user) {
        for (Follow follow : this.followRepository.findPendingByFollowingId(user.getUserId())) {
            follow.accept(user.getUserId());
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
