package com.example.userService.application.dto;

import com.example.userService.domain.enums.UserProfileStatus;

public record AssertUserActiveResponse(
        boolean active,
        UserProfileStatus status
) {
}
