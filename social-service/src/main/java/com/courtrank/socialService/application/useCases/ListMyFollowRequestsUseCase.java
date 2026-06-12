package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.FollowRequestSummary;
import com.courtrank.socialService.application.dto.ListFollowRequestsRequest;
import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.domain.entity.Follow;
import com.courtrank.socialService.domain.entity.SocialUser;
import com.courtrank.socialService.domain.repository.FollowRepository;
import com.courtrank.socialService.domain.repository.SocialUserRepository;

import java.util.List;
import java.util.Objects;

public class ListMyFollowRequestsUseCase extends SocialReadUseCaseSupport {
    private final FollowRepository followRepository;
    private final SocialUserRepository socialUserRepository;

    public ListMyFollowRequestsUseCase(
            FollowRepository followRepository,
            SocialUserRepository socialUserRepository
    ) {
        this.followRepository = followRepository;
        this.socialUserRepository = socialUserRepository;
    }

    public List<FollowRequestSummary> execute(ListFollowRequestsRequest request, TraceContext trace) {
        return this.followRepository.findPendingByFollowingId(request.ownerId())
                .stream()
                .map(this::toFollowRequestSummary)
                .filter(Objects::nonNull)
                .toList();
    }

    private FollowRequestSummary toFollowRequestSummary(Follow follow) {
        SocialUser follower = this.socialUserRepository.findByUserId(follow.getFollowerId()).orElse(null);
        if (follower == null || !follower.canBeShown()) {
            return null;
        }

        return new FollowRequestSummary(
                follow.getId(),
                this.toSummary(follower, follow.getCreatedAt()),
                follow.getCreatedAt()
        );
    }
}
