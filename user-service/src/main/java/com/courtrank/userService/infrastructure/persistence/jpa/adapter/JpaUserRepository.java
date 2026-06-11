package com.courtrank.userService.infrastructure.persistence.jpa.adapter;

import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.enums.UserProfileStatus;
import com.courtrank.userService.domain.repository.UserRepository;
import com.courtrank.userService.infrastructure.persistence.jpa.entity.UserJpaEntity;
import com.courtrank.userService.infrastructure.persistence.jpa.repository.SpringUserJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaUserRepository implements UserRepository {
    private final SpringUserJpaRepository repository;

    public JpaUserRepository(SpringUserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(User user) {
        this.repository.save(UserJpaEntity.fromDomain(user));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return this.repository.findById(id)
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String userName) {
        return this.repository.findByUsername(userName)
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public List<User> findByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return this.repository.findAllById(ids)
                .stream()
                .map(UserJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<User> searchPublic(String query, int limit, List<UUID> excludeIds) {
        String q = query == null ? "" : query.trim();
        int boundedLimit = Math.max(1, Math.min(limit, 50));

        if (q.length() < 2) {
            return List.of();
        }

        PageRequest page = PageRequest.of(0, boundedLimit);
        List<UserJpaEntity> users = excludeIds == null || excludeIds.isEmpty()
                ? this.repository.searchPublic(q, UserProfileStatus.VISIBLE, page)
                : this.repository.searchPublicWithExclusions(q, UserProfileStatus.VISIBLE, excludeIds, page);

        return users.stream()
                .map(UserJpaEntity::toDomain)
                .toList();
    }
}
