package com.example.userService.application.ports.audit;

public enum UserAuditEventType {
    USER_PROFILE_CREATED_FROM_AUTH_EVENT,
    USER_PROFILE_CREATION_SKIPPED_ALREADY_EXISTS,
    USER_PROFILE_CREATION_FAILED_USERNAME_CONFLICT
}
