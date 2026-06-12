package com.courtrank.socialService.infrastructure.persistence.jpa.repository;

import com.courtrank.socialService.domain.enums.FollowStatus;
import com.courtrank.socialService.infrastructure.persistence.jpa.entity.FollowJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringFollowJpaRepository extends JpaRepository<FollowJpaEntity, UUID> {
    Optional<FollowJpaEntity> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    boolean existsByFollowerIdAndFollowingIdAndStatus(UUID followerId, UUID followingId, FollowStatus status);

    List<FollowJpaEntity> findByFollowingId(UUID followingId);

    List<FollowJpaEntity> findByFollowerId(UUID followerId);

    List<FollowJpaEntity> findByFollowingIdAndStatus(UUID followingId, FollowStatus status);

    @Query("""
            select follow
            from FollowJpaEntity follow
            where (follow.followerId = :userA and follow.followingId = :userB)
               or (follow.followerId = :userB and follow.followingId = :userA)
            """)
    List<FollowJpaEntity> findBetweenUsers(@Param("userA") UUID userA, @Param("userB") UUID userB);
}
