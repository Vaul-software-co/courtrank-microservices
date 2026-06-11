package com.courtrank.userService.application.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record GetInternalUsersByIdsRequest(
        @NotEmpty
        List<UUID> userIds
) {
}
