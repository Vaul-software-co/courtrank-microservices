package com.courtrank.userService.application.dto;

import com.courtrank.userService.domain.enums.UserProfileStatus;

public record UserProfileStatusResponse(
        UserProfileStatus status
) {
}
