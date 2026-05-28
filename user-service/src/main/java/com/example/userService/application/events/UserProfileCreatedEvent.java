package com.example.userService.application.events;

import com.example.userService.domain.enums.UserProfileStatus;

import java.time.Instant;
import java.util.UUID;

public record UserProfileCreatedEvent(
        UUID id,
        String email,
        String name,
        String username,
        boolean privateProfile,
        UserProfileStatus status,
        Instant occurredAt
) {
}
