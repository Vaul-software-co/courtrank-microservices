package com.example.userService.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UnbanUserProfileRequest(
        @NotNull
        UUID adminUserId,

        @NotNull
        UUID targetUserId
) {
}
