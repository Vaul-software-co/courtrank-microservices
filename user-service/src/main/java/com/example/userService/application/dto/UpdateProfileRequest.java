package com.example.userService.application.dto;

import com.example.userService.domain.enums.UserGender;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
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
