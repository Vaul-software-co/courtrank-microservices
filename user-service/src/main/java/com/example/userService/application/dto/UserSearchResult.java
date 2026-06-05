package com.example.userService.application.dto;

import java.util.UUID;

public record UserSearchResult(
        UUID id,
        String name,
        String username,
        String avatarUrl
) {
}
