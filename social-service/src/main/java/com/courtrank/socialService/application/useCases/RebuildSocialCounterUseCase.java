package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.RebuildSocialCounterRequest;
import com.courtrank.socialService.domain.entity.SocialCounter;
import com.courtrank.socialService.domain.repository.BlockRepository;
import com.courtrank.socialService.domain.repository.FollowRepository;
import com.courtrank.socialService.domain.repository.SocialCounterRepository;

import java.time.Instant;

public class RebuildSocialCounterUseCase {
    private final FollowRepository followRepository;
    private final BlockRepository blockRepository;
    private final SocialCounterRepository socialCounterRepository;

    public RebuildSocialCounterUseCase(
            FollowRepository followRepository,
            BlockRepository blockRepository,
            SocialCounterRepository socialCounterRepository
    ) {
        this.followRepository = followRepository;
        this.blockRepository = blockRepository;
        this.socialCounterRepository = socialCounterRepository;
    }

    public SocialCounter execute(RebuildSocialCounterRequest request) {
        int followersCount = (int) this.followRepository.findFollowersByFollowingId(request.userId()).stream().filter(follow -> follow.isAccepted()).count();
        int followingCount = (int) this.followRepository.findFollowingByFollowerId(request.userId()).stream().filter(follow -> follow.isAccepted()).count();
        int pendingRequestsCount = this.followRepository.findPendingByFollowingId(request.userId()).size();
        int blockedCount = this.blockRepository.findByBlockerId(request.userId()).size();
        Instant now = Instant.now();

        SocialCounter rebuilt = SocialCounter.restore(
                request.userId(),
                followersCount,
                followingCount,
                pendingRequestsCount,
                blockedCount,
                now,
                now
        );
        return this.socialCounterRepository.save(rebuilt);
    }
}
