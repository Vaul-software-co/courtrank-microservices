package com.courtrank.userService.application.dto;

import com.courtrank.userService.domain.enums.UserGender;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateMyProfileRequest(
        @NotNull UUID userId,

        @Size(min = 1, max = 100)
        String name,

        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[a-zA-ZñÑ0-9_]+$", message = "Username can only contain letters, numbers and underscores")
        String username,

        @Size(min = 5, max = 20)
        String phoneNumber,

        UserGender gender
) {
}
