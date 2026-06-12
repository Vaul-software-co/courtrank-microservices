package com.courtrank.socialService.infrastructure.persistence.jpa.repository;

import com.courtrank.socialService.infrastructure.persistence.jpa.entity.SocialCounterJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringSocialCounterJpaRepository extends JpaRepository<SocialCounterJpaEntity, UUID> {
}
