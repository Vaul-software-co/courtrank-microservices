package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.RebuildSocialCounterRequest;
import com.courtrank.socialService.domain.entity.SocialCounter;
import com.courtrank.socialService.domain.repository.BlockRepository;
import com.courtrank.socialService.domain.repository.FollowRepository;
import com.courtrank.socialService.domain.repository.SocialCounterRepository;
import com.courtrank.socialService.domain.repository.SocialUserRepository;

import java.util.List;

public class RebuildAllSocialCountersUseCase {
    private final SocialUserRepository socialUserRepository;
    private final RebuildSocialCounterUseCase rebuildSocialCounterUseCase;

    public RebuildAllSocialCountersUseCase(
            SocialUserRepository socialUserRepository,
            FollowRepository followRepository,
            BlockRepository blockRepository,
            SocialCounterRepository socialCounterRepository
    ) {
        this.socialUserRepository = socialUserRepository;
        this.rebuildSocialCounterUseCase = new RebuildSocialCounterUseCase(followRepository, blockRepository, socialCounterRepository);
    }

    public List<SocialCounter> execute() {
        return this.socialUserRepository.findAllUserIds()
                .stream()
                .map(userId -> this.rebuildSocialCounterUseCase.execute(new RebuildSocialCounterRequest(userId)))
                .toList();
    }
}
