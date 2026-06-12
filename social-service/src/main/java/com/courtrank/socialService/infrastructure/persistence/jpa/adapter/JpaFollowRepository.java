package com.courtrank.socialService.infrastructure.persistence.jpa.adapter;

import com.courtrank.socialService.domain.entity.Follow;
import com.courtrank.socialService.domain.enums.FollowStatus;
import com.courtrank.socialService.domain.repository.FollowRepository;
import com.courtrank.socialService.infrastructure.persistence.jpa.entity.FollowJpaEntity;
import com.courtrank.socialService.infrastructure.persistence.jpa.repository.SpringFollowJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaFollowRepository implements FollowRepository {
    private final SpringFollowJpaRepository repository;

    public JpaFollowRepository(SpringFollowJpaRepository repository) {
        this.repository = repository;
    }

    @Override public Follow save(Follow follow) { return this.repository.save(FollowJpaEntity.fromDomain(follow)).toDomain(); }
    @Override public Optional<Follow> findById(UUID id) { return this.repository.findById(id).map(FollowJpaEntity::toDomain); }
    @Override public Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId) { return this.repository.findByFollowerIdAndFollowingId(followerId, followingId).map(FollowJpaEntity::toDomain); }
    @Override public boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId) { return this.repository.existsByFollowerIdAndFollowingId(followerId, followingId); }
    @Override public boolean existsAcceptedByFollowerIdAndFollowingId(UUID followerId, UUID followingId) { return this.repository.existsByFollowerIdAndFollowingIdAndStatus(followerId, followingId, FollowStatus.ACCEPTED); }
    @Override public List<Follow> findFollowersByFollowingId(UUID followingId) { return this.repository.findByFollowingId(followingId).stream().map(FollowJpaEntity::toDomain).toList(); }
    @Override public List<Follow> findFollowingByFollowerId(UUID followerId) { return this.repository.findByFollowerId(followerId).stream().map(FollowJpaEntity::toDomain).toList(); }
    @Override public List<Follow> findPendingByFollowingId(UUID followingId) { return this.repository.findByFollowingIdAndStatus(followingId, FollowStatus.PENDING).stream().map(FollowJpaEntity::toDomain).toList(); }
    @Override public List<Follow> findBetweenUsers(UUID userA, UUID userB) { return this.repository.findBetweenUsers(userA, userB).stream().map(FollowJpaEntity::toDomain).toList(); }
    @Override public void delete(Follow follow) { this.repository.deleteById(follow.getId()); }
}
