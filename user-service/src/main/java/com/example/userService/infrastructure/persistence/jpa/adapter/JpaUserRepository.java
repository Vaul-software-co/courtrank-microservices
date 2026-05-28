package com.example.userService.infrastructure.persistence.jpa.adapter;

import com.example.userService.domain.entity.User;
import com.example.userService.domain.repository.UserRepository;
import com.example.userService.infrastructure.persistence.jpa.entity.UserJpaEntity;
import com.example.userService.infrastructure.persistence.jpa.repository.SpringUserJpaRepository;
import org.springframework.stereotype.Repository;

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
}
