package com.example.userService.infrastructure.persistence.jpa.repository;

import com.example.userService.domain.enums.UserProfileStatus;
import com.example.userService.infrastructure.persistence.jpa.entity.UserJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringUserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
    @Query("""
            select user
            from UserJpaEntity user
            where lower(user.userName) = lower(:username)
            """)
    Optional<UserJpaEntity> findByUsername(@Param("username") String username);

    @Query("""
            select user
            from UserJpaEntity user
            where user.status = :status
              and (
                lower(user.name) like lower(concat('%', :query, '%'))
                or lower(user.userName) like lower(concat('%', :query, '%'))
              )
              and user.id not in :excludeIds
            order by user.name asc
            """)
    List<UserJpaEntity> searchPublicWithExclusions(
            @Param("query") String query,
            @Param("status") UserProfileStatus status,
            @Param("excludeIds") List<UUID> excludeIds,
            Pageable pageable
    );

    @Query("""
            select user
            from UserJpaEntity user
            where user.status = :status
              and (
                lower(user.name) like lower(concat('%', :query, '%'))
                or lower(user.userName) like lower(concat('%', :query, '%'))
              )
            order by user.name asc
            """)
    List<UserJpaEntity> searchPublic(
            @Param("query") String query,
            @Param("status") UserProfileStatus status,
            Pageable pageable
    );
}
