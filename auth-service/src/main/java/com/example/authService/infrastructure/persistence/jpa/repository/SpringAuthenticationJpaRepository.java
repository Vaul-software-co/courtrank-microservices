package com.example.authService.infrastructure.persistence.jpa.repository;

import com.example.authService.infrastructure.persistence.jpa.entity.AuthenticationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringAuthenticationJpaRepository extends JpaRepository<AuthenticationJpaEntity, UUID> {
    @Query("""
            select auth
            from AuthenticationJpaEntity auth
            where lower(auth.email) = lower(:email)
              and auth.deletedAt is null
            """)
    Optional<AuthenticationJpaEntity> findByEmailAndDeletedAtIsNull(@Param("email") String email);

    @Query("""
            select auth
            from AuthenticationJpaEntity auth
            where lower(auth.email) = lower(:email)
            """)
    Optional<AuthenticationJpaEntity> findByEmail(@Param("email") String email);
}
