package com.courtrank.userService.application.useCases;

import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

public class CheckUsernameAvailabilityUseCase {
    private final UserRepository userRepository;

    public CheckUsernameAvailabilityUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean execute(String username, UUID userId) {
        Optional<User> user = this.userRepository.findByUsername(username);

        return user.isEmpty() || user.orElseThrow().getId().equals(userId);
    }
}
