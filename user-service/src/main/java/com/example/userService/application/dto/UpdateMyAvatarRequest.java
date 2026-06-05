package com.example.userService.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateMyAvatarRequest(
        @NotNull
        UUID userId,

        @NotBlank
        String avatarKey
) {
}
