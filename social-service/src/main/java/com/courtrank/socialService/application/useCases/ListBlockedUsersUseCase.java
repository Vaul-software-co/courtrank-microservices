package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.BlockedUserSummary;
import com.courtrank.socialService.application.dto.ListBlockedUsersRequest;
import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.domain.entity.Block;
import com.courtrank.socialService.domain.entity.SocialUser;
import com.courtrank.socialService.domain.repository.BlockRepository;
import com.courtrank.socialService.domain.repository.SocialUserRepository;

import java.util.List;
import java.util.Objects;

public class ListBlockedUsersUseCase extends SocialReadUseCaseSupport {
    private final BlockRepository blockRepository;
    private final SocialUserRepository socialUserRepository;

    public ListBlockedUsersUseCase(BlockRepository blockRepository, SocialUserRepository socialUserRepository) {
        this.blockRepository = blockRepository;
        this.socialUserRepository = socialUserRepository;
    }

    public List<BlockedUserSummary> execute(ListBlockedUsersRequest request, TraceContext trace) {
        return this.blockRepository.findByBlockerId(request.blockerId())
                .stream()
                .map(this::toBlockedUserSummary)
                .filter(Objects::nonNull)
                .toList();
    }

    private BlockedUserSummary toBlockedUserSummary(Block block) {
        SocialUser blocked = this.socialUserRepository.findByUserId(block.getBlockedId()).orElse(null);
        if (blocked == null || !blocked.canBeShown()) {
            return null;
        }
        return new BlockedUserSummary(this.toSummary(blocked, block.getCreatedAt()), block.getCreatedAt());
    }
}
