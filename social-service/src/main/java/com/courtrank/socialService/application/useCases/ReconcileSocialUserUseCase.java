package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.ReconcileSocialUserRequest;
import com.courtrank.socialService.application.dto.SyncSocialUserRequest;
import com.courtrank.socialService.application.ports.SocialUserProfileProvider;
import com.courtrank.socialService.domain.exceptions.SocialUserNotFoundException;

public class ReconcileSocialUserUseCase {
    private final SocialUserProfileProvider socialUserProfileProvider;
    private final UpdateSocialUserFromUserEventUseCase updateSocialUserFromUserEventUseCase;

    public ReconcileSocialUserUseCase(
            SocialUserProfileProvider socialUserProfileProvider,
            UpdateSocialUserFromUserEventUseCase updateSocialUserFromUserEventUseCase
    ) {
        this.socialUserProfileProvider = socialUserProfileProvider;
        this.updateSocialUserFromUserEventUseCase = updateSocialUserFromUserEventUseCase;
    }

    public void execute(ReconcileSocialUserRequest request) {
        var snapshot = this.socialUserProfileProvider
                .findByUserId(request.userId())
                .orElseThrow(SocialUserNotFoundException::new);
        this.updateSocialUserFromUserEventUseCase.execute(new SyncSocialUserRequest(snapshot));
    }
}
