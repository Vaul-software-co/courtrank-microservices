package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.ListFollowingRequest;
import com.courtrank.socialService.application.dto.SocialUserSummary;
import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.domain.entity.Follow;
import com.courtrank.socialService.domain.entity.SocialUser;
import com.courtrank.socialService.domain.exceptions.SocialUserNotFoundException;
import com.courtrank.socialService.domain.repository.BlockRepository;
import com.courtrank.socialService.domain.repository.FollowRepository;
import com.courtrank.socialService.domain.repository.SocialUserRepository;

import java.util.List;
import java.util.Objects;

public class ListFollowingUseCase extends SocialReadUseCaseSupport {
    private final FollowRepository followRepository;
    private final SocialUserRepository socialUserRepository;
    private final BlockRepository blockRepository;

    public ListFollowingUseCase(FollowRepository followRepository, SocialUserRepository socialUserRepository, BlockRepository blockRepository) {
        this.followRepository = followRepository;
        this.socialUserRepository = socialUserRepository;
        this.blockRepository = blockRepository;
    }

    public List<SocialUserSummary> execute(ListFollowingRequest request, TraceContext trace) {
        SocialUser target = this.socialUserRepository.findByUserId(request.targetId()).orElse(null);
        if (target == null || !target.canBeShown()) {
            throw new SocialUserNotFoundException();
        }

        if (!canViewList(request.viewerId(), target)) {
            return List.of();
        }

        return this.followRepository.findFollowingByFollowerId(request.targetId())
                .stream()
                .filter(Follow::isAccepted)
                .map(this::toFollowingSummary)
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean canViewList(java.util.UUID viewerId, SocialUser target) {
        if (viewerId.equals(target.getUserId())) {
            return true;
        }
        if (this.blockRepository.existsBetweenUsers(viewerId, target.getUserId())) {
            throw new SocialUserNotFoundException();
        }
        return !target.isPrivate() || this.followRepository.existsAcceptedByFollowerIdAndFollowingId(viewerId, target.getUserId());
    }

    private SocialUserSummary toFollowingSummary(Follow follow) {
        SocialUser following = this.socialUserRepository.findByUserId(follow.getFollowingId()).orElse(null);
        if (following == null || !following.canBeShown()) {
            return null;
        }
        return this.toSummary(following, follow.getAcceptedAt() != null ? follow.getAcceptedAt() : follow.getCreatedAt());
    }
}
