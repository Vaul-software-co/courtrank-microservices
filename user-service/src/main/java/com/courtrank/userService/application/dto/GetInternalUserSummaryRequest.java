package com.courtrank.userService.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GetInternalUserSummaryRequest(
        @NotNull
        UUID userId
) {
}
