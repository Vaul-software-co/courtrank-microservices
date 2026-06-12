package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.AreUsersBlockedRequest;
import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.domain.repository.BlockRepository;

public class AreUsersBlockedUseCase {
    private final BlockRepository blockRepository;

    public AreUsersBlockedUseCase(BlockRepository blockRepository) {
        this.blockRepository = blockRepository;
    }

    public boolean execute(AreUsersBlockedRequest request, TraceContext trace) {
        return this.blockRepository.existsBetweenUsers(request.userA(), request.userB());
    }
}
