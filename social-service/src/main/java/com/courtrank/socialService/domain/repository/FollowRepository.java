package com.courtrank.socialService.domain.repository;

import com.courtrank.socialService.domain.entity.Follow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowRepository {
    Follow save(Follow follow);

    Optional<Follow> findById(UUID id);

    Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    boolean existsAcceptedByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    List<Follow> findFollowersByFollowingId(UUID followingId);

    List<Follow> findFollowingByFollowerId(UUID followerId);

    List<Follow> findPendingByFollowingId(UUID followingId);

    List<Follow> findBetweenUsers(UUID userA, UUID userB);

    void delete(Follow follow);
}