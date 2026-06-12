package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.GetRelatedBlockedUserIdsRequest;
import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.domain.repository.BlockRepository;

import java.util.Set;
import java.util.UUID;

public class GetRelatedBlockedUserIdsUseCase {
    private final BlockRepository blockRepository;

    public GetRelatedBlockedUserIdsUseCase(BlockRepository blockRepository) {
        this.blockRepository = blockRepository;
    }

    public Set<UUID> execute(GetRelatedBlockedUserIdsRequest request, TraceContext trace) {
        return this.blockRepository.findRelatedUserIds(request.userId());
    }
}
