package com.courtrank.socialService.infrastructure.persistence.jpa.adapter;

import com.courtrank.socialService.domain.entity.SocialUser;
import com.courtrank.socialService.domain.repository.SocialUserRepository;
import com.courtrank.socialService.infrastructure.persistence.jpa.entity.SocialUserJpaEntity;
import com.courtrank.socialService.infrastructure.persistence.jpa.repository.SpringSocialUserJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class JpaSocialUserRepository implements SocialUserRepository {
    private final SpringSocialUserJpaRepository repository;

    public JpaSocialUserRepository(SpringSocialUserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(SocialUser socialUser) {
        this.repository.save(SocialUserJpaEntity.fromDomain(socialUser));
    }

    @Override
    public Optional<SocialUser> findByUserId(UUID userId) {
        return this.repository.findById(userId).map(SocialUserJpaEntity::toDomain);
    }

    @Override
    public List<UUID> findAllUserIds() {
        return this.repository.findAllUserIds();
    }

    @Override
    public List<SocialUser> searchVisible(String query, int limit, Set<UUID> excludedUserIds) {
        String q = query == null ? "" : query.trim();
        int boundedLimit = Math.max(1, Math.min(limit, 50));
        if (q.length() < 2) {
            return List.of();
        }

        PageRequest page = PageRequest.of(0, boundedLimit);
        List<SocialUserJpaEntity> users = excludedUserIds == null || excludedUserIds.isEmpty()
                ? this.repository.searchVisible(q, page)
                : this.repository.searchVisibleWithExclusions(q, List.copyOf(excludedUserIds), page);

        return users.stream().map(SocialUserJpaEntity::toDomain).toList();
    }
}
