package com.courtrank.socialService.application.ports;

import com.courtrank.socialService.application.dto.SocialUserSnapshot;

import java.util.Optional;
import java.util.UUID;

public interface SocialUserProfileProvider {
    Optional<SocialUserSnapshot> findByUserId(UUID userId);
}
