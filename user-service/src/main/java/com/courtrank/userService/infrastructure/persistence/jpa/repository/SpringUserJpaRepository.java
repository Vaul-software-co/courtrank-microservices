package com.courtrank.userService.infrastructure.persistence.jpa.repository;

import com.courtrank.userService.infrastructure.persistence.jpa.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringUserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
    @Query("""
            select user
            from UserJpaEntity user
            where lower(user.userName) = lower(:username)
            """)
    Optional<UserJpaEntity> findByUsername(@Param("username") String username);
}
