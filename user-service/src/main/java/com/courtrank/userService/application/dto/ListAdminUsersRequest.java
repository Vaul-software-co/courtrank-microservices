package com.courtrank.userService.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ListAdminUsersRequest(
        String query,
        @Min(1)
        @Max(100)
        int limit,
        @Min(0)
        int offset
) {
}
