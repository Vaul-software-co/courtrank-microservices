package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.GetUserSocialSummaryRequest;
import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.application.dto.UserSocialSummaryResponse;
import com.courtrank.socialService.domain.entity.SocialCounter;
import com.courtrank.socialService.domain.entity.SocialUser;
import com.courtrank.socialService.domain.enums.FollowStatus;
import com.courtrank.socialService.domain.enums.ViewerFollowStatus;
import com.courtrank.socialService.domain.exceptions.SocialUserNotFoundException;
import com.courtrank.socialService.domain.repository.BlockRepository;
import com.courtrank.socialService.domain.repository.FollowRepository;
import com.courtrank.socialService.domain.repository.SocialCounterRepository;
import com.courtrank.socialService.domain.repository.SocialUserRepository;

public class GetUserSocialSummaryUseCase {
    private final SocialUserRepository socialUserRepository;
    private final FollowRepository followRepository;
    private final BlockRepository blockRepository;
    private final SocialCounterRepository socialCounterRepository;

    public GetUserSocialSummaryUseCase(
            SocialUserRepository socialUserRepository,
            FollowRepository followRepository,
            BlockRepository blockRepository,
            SocialCounterRepository socialCounterRepository
    ) {
        this.socialUserRepository = socialUserRepository;
        this.followRepository = followRepository;
        this.blockRepository = blockRepository;
        this.socialCounterRepository = socialCounterRepository;
    }

    public UserSocialSummaryResponse execute(GetUserSocialSummaryRequest request, TraceContext trace) {
        SocialUser target = this.socialUserRepository.findByUserId(request.targetId()).orElse(null);
        if (target == null || !target.canBeShown()) {
            throw new SocialUserNotFoundException();
        }

        boolean blocked = !request.viewerId().equals(request.targetId())
                && this.blockRepository.existsBetweenUsers(request.viewerId(), request.targetId());
        if (blocked) {
            throw new SocialUserNotFoundException();
        }

        ViewerFollowStatus status = this.followRepository
                .findByFollowerIdAndFollowingId(request.viewerId(), request.targetId())
                .map(follow -> follow.getFollowStatus() == FollowStatus.ACCEPTED ? ViewerFollowStatus.ACCEPTED : ViewerFollowStatus.PENDING)
                .orElse(ViewerFollowStatus.NONE);
        SocialCounter counter = this.socialCounterRepository.findByUserId(request.targetId()).orElseGet(() -> SocialCounter.create(request.targetId()));

        return new UserSocialSummaryResponse(
                target.getUserId(),
                target.getName(),
                target.getUsername(),
                target.getAvatarUrl(),
                target.isPrivate(),
                status,
                false,
                counter.getFollowersCount(),
                counter.getFollowingCount(),
                counter.getPendingRequestsCount(),
                counter.getBlockedCount(),
                target.getCreatedAt()
        );
    }
}
