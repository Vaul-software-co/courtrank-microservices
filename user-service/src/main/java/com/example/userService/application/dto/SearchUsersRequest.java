package com.example.userService.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record SearchUsersRequest(
        @NotBlank
        @Size(min = 2, max = 100)
        String query,

        @Min(1)
        @Max(50)
        int limit,

        List<UUID> excludeIds
) {
}
