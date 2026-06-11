package com.courtrank.authService.domain.repository;

import com.courtrank.authService.domain.entity.Authentication;

import java.util.Optional;
import java.util.UUID;

public interface AuthenticationRepository {
    void save(Authentication authentication);
    Optional<Authentication> findByEmail(String email);
    Optional<Authentication> findByEmailIncludingDeleted(String email);
    Optional<Authentication> findById(UUID id);
}
