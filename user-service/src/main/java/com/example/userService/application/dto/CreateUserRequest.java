package com.example.userService.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateUserRequest(
        @NotNull
        UUID id,

        @NotBlank
        String name,

        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[a-zA-ZñÑ0-9_]+$", message = "Username can only contain letters, numbers and underscores")
    String userName,

    @NotBlank
    String email,

    boolean emailVerified
) {
}
