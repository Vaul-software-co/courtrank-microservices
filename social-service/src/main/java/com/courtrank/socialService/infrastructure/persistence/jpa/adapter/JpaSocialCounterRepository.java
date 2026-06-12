package com.courtrank.socialService.infrastructure.persistence.jpa.adapter;

import com.courtrank.socialService.domain.entity.SocialCounter;
import com.courtrank.socialService.domain.repository.SocialCounterRepository;
import com.courtrank.socialService.infrastructure.persistence.jpa.entity.SocialCounterJpaEntity;
import com.courtrank.socialService.infrastructure.persistence.jpa.repository.SpringSocialCounterJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaSocialCounterRepository implements SocialCounterRepository {
    private final SpringSocialCounterJpaRepository repository;

    public JpaSocialCounterRepository(SpringSocialCounterJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public SocialCounter save(SocialCounter socialCounter) {
        return this.repository.save(SocialCounterJpaEntity.fromDomain(socialCounter)).toDomain();
    }

    @Override
    public Optional<SocialCounter> findByUserId(UUID userId) {
        return this.repository.findById(userId).map(SocialCounterJpaEntity::toDomain);
    }
}
