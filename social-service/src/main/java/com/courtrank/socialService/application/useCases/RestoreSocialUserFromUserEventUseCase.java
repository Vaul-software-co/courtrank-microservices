package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.SyncSocialUserRequest;
import com.courtrank.socialService.domain.entity.SocialCounter;
import com.courtrank.socialService.domain.entity.SocialUser;
import com.courtrank.socialService.domain.repository.SocialCounterRepository;
import com.courtrank.socialService.domain.repository.SocialUserRepository;

public class RestoreSocialUserFromUserEventUseCase {
    private final SocialUserRepository socialUserRepository;
    private final SocialCounterRepository socialCounterRepository;

    public RestoreSocialUserFromUserEventUseCase(SocialUserRepository socialUserRepository, SocialCounterRepository socialCounterRepository) {
        this.socialUserRepository = socialUserRepository;
        this.socialCounterRepository = socialCounterRepository;
    }

    public void execute(SyncSocialUserRequest request) {
        var snapshot = request.snapshot();
        SocialUser user = this.socialUserRepository.findByUserId(snapshot.userId()).orElse(null);
        if (user == null) {
            user = SocialUser.create(snapshot.userId(), snapshot.name(), snapshot.username(), snapshot.avatarUrl(), snapshot.privateProfile(), snapshot.active(), snapshot.sourceUpdatedAt());
        } else {
            user.restoreFromSource(snapshot.name(), snapshot.username(), snapshot.avatarUrl(), snapshot.privateProfile(), snapshot.active(), snapshot.sourceUpdatedAt());
        }
        this.socialUserRepository.save(user);
        if (this.socialCounterRepository.findByUserId(snapshot.userId()).isEmpty()) {
            this.socialCounterRepository.save(SocialCounter.create(snapshot.userId()));
        }
    }
}
