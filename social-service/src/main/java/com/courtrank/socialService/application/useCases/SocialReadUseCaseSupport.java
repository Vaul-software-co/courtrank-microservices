package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.SocialUserSummary;
import com.courtrank.socialService.domain.entity.SocialUser;

import java.time.Instant;

abstract class SocialReadUseCaseSupport {
    protected SocialUserSummary toSummary(SocialUser user, Instant relatedAt) {
        return new SocialUserSummary(
                user.getUserId(),
                user.getName(),
                user.getUsername(),
                user.getAvatarUrl(),
                relatedAt
        );
    }
}
