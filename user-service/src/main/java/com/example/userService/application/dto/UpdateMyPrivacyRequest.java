package com.example.userService.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateMyPrivacyRequest(
        @NotNull
        UUID userId,

        @NotNull
        Boolean privateProfile
) {
}
