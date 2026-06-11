package com.courtrank.userService.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BanUserProfileRequest(
        @NotNull
        UUID adminUserId,

        @NotNull
        UUID targetUserId
) {
}
