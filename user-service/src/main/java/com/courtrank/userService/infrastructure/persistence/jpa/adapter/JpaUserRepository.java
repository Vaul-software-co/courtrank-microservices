package com.courtrank.userService.infrastructure.persistence.jpa.adapter;

import com.courtrank.userService.domain.entity.User;
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
    public List<User> searchForAdmin(String query, int limit, int offset) {
        String normalizedQuery = this.normalizeQuery(query);
        int page = Math.max(0, offset / limit);
        var pageable = PageRequest.of(page, limit);
        var users = normalizedQuery == null
                ? this.repository.findAllForAdmin(pageable)
                : this.repository.searchForAdmin(normalizedQuery, pageable);

        return users
                .stream()
                .map(UserJpaEntity::toDomain)
                .toList();
    }

    @Override
    public long countForAdmin(String query) {
        String normalizedQuery = this.normalizeQuery(query);
        return normalizedQuery == null ? this.repository.count() : this.repository.countForAdmin(normalizedQuery);
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim().toLowerCase();
    }
}
