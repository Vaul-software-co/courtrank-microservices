package com.courtrank.userService.application.dto;

import java.util.List;

public record ListAdminUsersResponse(
        List<AdminUserSummaryResponse> users,
        int limit,
        int offset,
        long total
) {
}
