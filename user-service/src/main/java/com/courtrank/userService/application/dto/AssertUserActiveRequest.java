package com.courtrank.userService.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssertUserActiveRequest(
        @NotNull
        UUID userId
) {
}
