package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.DeleteSocialUserRequest;
import com.courtrank.socialService.domain.entity.SocialUser;
import com.courtrank.socialService.domain.repository.SocialUserRepository;

public class DeleteSocialUserFromUserEventUseCase {
    private final SocialUserRepository socialUserRepository;

    public DeleteSocialUserFromUserEventUseCase(SocialUserRepository socialUserRepository) {
        this.socialUserRepository = socialUserRepository;
    }

    public void execute(DeleteSocialUserRequest request) {
        SocialUser user = this.socialUserRepository.findByUserId(request.userId()).orElse(null);
        if (user == null) {
            return;
        }

        user.markDeleted(request.deletedAt(), request.sourceUpdatedAt());
        this.socialUserRepository.save(user);
    }
}
