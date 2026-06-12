package com.courtrank.socialService.domain.repository;

import com.courtrank.socialService.domain.entity.SocialUser;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SocialUserRepository {
    void save(SocialUser socialUser);
    Optional<SocialUser> findByUserId(UUID userId);
    List<UUID> findAllUserIds();
    List<SocialUser> searchVisible(String query, int limit, Set<UUID> excludedUserIds);
}
