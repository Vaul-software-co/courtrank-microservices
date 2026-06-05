package com.example.userService.application.dto;

import com.example.userService.domain.enums.UserProfileStatus;

public record UserProfileStatusResponse(
        UserProfileStatus status
) {
}
