package com.example.authService.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeleteUserRequest(
        @NotNull
        UUID userId
) {
}
