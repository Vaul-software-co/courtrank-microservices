package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.SearchSocialUsersRequest;
import com.courtrank.socialService.application.dto.SocialUserSummary;
import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.domain.repository.BlockRepository;
import com.courtrank.socialService.domain.repository.SocialUserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SearchSocialUsersUseCase extends SocialReadUseCaseSupport {
    private final SocialUserRepository socialUserRepository;
    private final BlockRepository blockRepository;

    public SearchSocialUsersUseCase(SocialUserRepository socialUserRepository, BlockRepository blockRepository) {
        this.socialUserRepository = socialUserRepository;
        this.blockRepository = blockRepository;
    }

    public List<SocialUserSummary> execute(SearchSocialUsersRequest request, TraceContext trace) {
        String query = request.query() == null ? "" : request.query().trim();
        if (query.length() < 2) {
            return List.of();
        }

        Set<UUID> excluded = new HashSet<>(this.blockRepository.findRelatedUserIds(request.viewerId()));
        excluded.add(request.viewerId());

        int limit = Math.max(1, Math.min(request.limit(), 50));
        return this.socialUserRepository.searchVisible(query, limit, excluded)
                .stream()
                .map(user -> this.toSummary(user, user.getUpdatedAt()))
                .toList();
    }
}
