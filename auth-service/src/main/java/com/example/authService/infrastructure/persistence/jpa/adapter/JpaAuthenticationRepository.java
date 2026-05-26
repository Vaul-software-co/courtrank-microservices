package com.example.authService.infrastructure.persistence.jpa.adapter;

import com.example.authService.domain.entity.Authentication;
import com.example.authService.domain.repository.AuthenticationRepository;
import com.example.authService.infrastructure.persistence.jpa.entity.AuthenticationJpaEntity;
import com.example.authService.infrastructure.persistence.jpa.repository.SpringAuthenticationJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaAuthenticationRepository implements AuthenticationRepository {
    private final SpringAuthenticationJpaRepository repository;

    public JpaAuthenticationRepository(SpringAuthenticationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(Authentication authentication) {
        this.repository.save(AuthenticationJpaEntity.fromDomain(authentication));
    }

    @Override
    public Optional<Authentication> findByEmail(String email) {
        return this.repository.findByEmailAndDeletedAtIsNull(email)
                .map(AuthenticationJpaEntity::toDomain);
    }

    @Override
    public Optional<Authentication> findByEmailIncludingDeleted(String email) {
        return this.repository.findByEmail(email)
                .map(AuthenticationJpaEntity::toDomain);
    }

    @Override
    public Optional<Authentication> findById(UUID id) {
        return this.repository.findById(id)
                .map(AuthenticationJpaEntity::toDomain);
    }
}
