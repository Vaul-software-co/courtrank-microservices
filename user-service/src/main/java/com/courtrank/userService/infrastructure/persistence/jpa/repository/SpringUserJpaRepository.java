package com.courtrank.userService.infrastructure.persistence.jpa.repository;

import com.courtrank.userService.infrastructure.persistence.jpa.entity.UserJpaEntity;
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
            order by user.createdAt desc, user.id desc
            """)
    List<UserJpaEntity> findAllForAdmin(Pageable pageable);

    @Query("""
            select user
            from UserJpaEntity user
            where lower(user.name) like concat('%', :query, '%')
               or lower(user.userName) like concat('%', :query, '%')
               or lower(user.email) like concat('%', :query, '%')
               or cast(user.id as string) like concat('%', :query, '%')
            order by user.createdAt desc, user.id desc
            """)
    List<UserJpaEntity> searchForAdmin(@Param("query") String query, Pageable pageable);

    @Query("""
            select count(user)
            from UserJpaEntity user
            where lower(user.name) like concat('%', :query, '%')
               or lower(user.userName) like concat('%', :query, '%')
               or lower(user.email) like concat('%', :query, '%')
               or cast(user.id as string) like concat('%', :query, '%')
            """)
    long countForAdmin(@Param("query") String query);
}
