package com.example.userService.domain.repository;

import com.example.userService.domain.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    void save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByUsername(String userName);
    List<User> findByIds(List<UUID> ids);
    List<User> searchPublic(String query, int limit, List<UUID> excludeIds);
}
