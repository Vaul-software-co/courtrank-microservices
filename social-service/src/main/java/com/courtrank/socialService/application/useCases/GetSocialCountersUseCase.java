package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.GetSocialCountersRequest;
import com.courtrank.socialService.application.dto.SocialCountersResponse;
import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.domain.entity.SocialCounter;
import com.courtrank.socialService.domain.repository.SocialCounterRepository;

public class GetSocialCountersUseCase {
    private final SocialCounterRepository socialCounterRepository;

    public GetSocialCountersUseCase(SocialCounterRepository socialCounterRepository) {
        this.socialCounterRepository = socialCounterRepository;
    }

    public SocialCountersResponse execute(GetSocialCountersRequest request, TraceContext trace) {
        SocialCounter counter = this.socialCounterRepository
                .findByUserId(request.userId())
                .orElseGet(() -> SocialCounter.create(request.userId()));

        return new SocialCountersResponse(
                counter.getUserId(),
                counter.getFollowersCount(),
                counter.getFollowingCount(),
                counter.getPendingRequestsCount(),
                counter.getBlockedCount()
        );
    }
}
