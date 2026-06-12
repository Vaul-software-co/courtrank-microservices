package com.courtrank.socialService.infrastructure.persistence.jpa.repository;

import com.courtrank.socialService.infrastructure.persistence.jpa.entity.SocialUserJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringSocialUserJpaRepository extends JpaRepository<SocialUserJpaEntity, UUID> {
    @Query("""
            select user
            from SocialUserJpaEntity user
            where user.active = true
              and user.deletedAt is null
              and user.userId not in :excludedUserIds
              and (
                lower(user.name) like lower(concat('%', :query, '%'))
                or lower(user.username) like lower(concat('%', :query, '%'))
              )
            order by user.name asc
            """)
    List<SocialUserJpaEntity> searchVisibleWithExclusions(
            @Param("query") String query,
            @Param("excludedUserIds") List<UUID> excludedUserIds,
            Pageable pageable
    );

    @Query("""
            select user
            from SocialUserJpaEntity user
            where user.active = true
              and user.deletedAt is null
              and (
                lower(user.name) like lower(concat('%', :query, '%'))
                or lower(user.username) like lower(concat('%', :query, '%'))
              )
            order by user.name asc
            """)
    List<SocialUserJpaEntity> searchVisible(
            @Param("query") String query,
            Pageable pageable
    );

    @Query("select user.userId from SocialUserJpaEntity user")
    List<UUID> findAllUserIds();
}
