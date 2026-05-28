package com.example.userService.domain.repository;

import com.example.userService.domain.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    void save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByUsername(String userName);
}
