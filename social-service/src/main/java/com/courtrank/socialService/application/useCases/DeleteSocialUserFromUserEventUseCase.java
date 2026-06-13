package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.DeleteSocialUserRequest;
import com.courtrank.socialService.domain.entity.Follow;
import com.courtrank.socialService.domain.entity.SocialCounter;
import com.courtrank.socialService.domain.entity.SocialUser;
import com.courtrank.socialService.domain.repository.FollowRepository;
import com.courtrank.socialService.domain.repository.SocialCounterRepository;
import com.courtrank.socialService.domain.repository.SocialUserRepository;

import java.util.ArrayList;
import java.util.List;

public class DeleteSocialUserFromUserEventUseCase extends SocialUseCaseSupport {
    private final SocialUserRepository socialUserRepository;
    private final FollowRepository followRepository;
    private final SocialCounterRepository socialCounterRepository;

    public DeleteSocialUserFromUserEventUseCase(
            SocialUserRepository socialUserRepository,
            FollowRepository followRepository,
            SocialCounterRepository socialCounterRepository
    ) {
        this.socialUserRepository = socialUserRepository;
        this.followRepository = followRepository;
        this.socialCounterRepository = socialCounterRepository;
    }

    public void execute(DeleteSocialUserRequest request) {
        SocialUser user = this.socialUserRepository.findByUserId(request.userId()).orElse(null);
        if (user == null) {
            return;
        }

        user.markDeleted(request.deletedAt(), request.sourceUpdatedAt());
        this.socialUserRepository.save(user);

        List<Follow> follows = new ArrayList<>();
        follows.addAll(this.followRepository.findFollowersByFollowingId(request.userId()));
        follows.addAll(this.followRepository.findFollowingByFollowerId(request.userId()));

        for (Follow follow : follows) {
            this.removeFollowFromCounters(follow);
            this.followRepository.delete(follow);
        }
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
