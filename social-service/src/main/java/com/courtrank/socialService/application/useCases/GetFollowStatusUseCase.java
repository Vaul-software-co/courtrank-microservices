package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.GetFollowStatusRequest;
import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.domain.enums.FollowStatus;
import com.courtrank.socialService.domain.enums.ViewerFollowStatus;
import com.courtrank.socialService.domain.repository.FollowRepository;

public class GetFollowStatusUseCase {
    private final FollowRepository followRepository;

    public GetFollowStatusUseCase(FollowRepository followRepository) {
        this.followRepository = followRepository;
    }

    public ViewerFollowStatus execute(GetFollowStatusRequest request, TraceContext trace) {
        return this.followRepository
                .findByFollowerIdAndFollowingId(request.followerId(), request.followingId())
                .map(follow -> follow.getFollowStatus() == FollowStatus.ACCEPTED
                        ? ViewerFollowStatus.ACCEPTED
                        : ViewerFollowStatus.PENDING)
                .orElse(ViewerFollowStatus.NONE);
    }
}
