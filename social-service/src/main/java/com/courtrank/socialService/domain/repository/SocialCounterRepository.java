package com.courtrank.socialService.domain.repository;

import com.courtrank.socialService.domain.entity.SocialCounter;

import java.util.Optional;
import java.util.UUID;

public interface SocialCounterRepository {
    SocialCounter save(SocialCounter socialCounter);

    Optional<SocialCounter> findByUserId(UUID userId);
}
