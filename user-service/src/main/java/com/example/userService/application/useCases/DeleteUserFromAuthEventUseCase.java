package com.example.userService.application.useCases;

import com.example.userService.application.ports.audit.UserAuditEvent;
import com.example.userService.application.ports.audit.UserAuditEventType;
import com.example.userService.application.ports.audit.UserAuditLogger;
import com.example.userService.domain.entity.User;
import com.example.userService.domain.enums.UserProfileStatus;
import com.example.userService.domain.repository.UserRepository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class DeleteUserFromAuthEventUseCase {
    private final UserRepository userRepository;
    private final UserAuditLogger auditLogger;

    public DeleteUserFromAuthEventUseCase(UserRepository userRepository, UserAuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
    }

    public void execute(UUID userId) {
        User user = this.userRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            this.auditLogger.log(new UserAuditEvent(
                    UserAuditEventType.USER_PROFILE_DELETION_SKIPPED_NOT_FOUND,
                    null,
                    userId,
                    null,
                    Map.of(),
                    Instant.now()
            ));
            return;
        }

        if (user.getStatus() == UserProfileStatus.DELETED) {
            this.auditLogger.log(new UserAuditEvent(
                    UserAuditEventType.USER_PROFILE_DELETION_SKIPPED_ALREADY_DELETED,
                    null,
                    user.getId(),
                    null,
                    Map.of("email", user.getEmail()),
                    Instant.now()
            ));
            return;
        }

        String releasedUsername = user.getUserName();
        user.markProfileAsDeleted();
        this.userRepository.save(user);
        this.auditLogger.log(new UserAuditEvent(
                UserAuditEventType.USER_PROFILE_DELETED_FROM_AUTH_EVENT,
                null,
                user.getId(),
                null,
                Map.of(
                        "email", user.getEmail(),
                        "releasedUsername", String.valueOf(releasedUsername)
                ),
                Instant.now()
        ));
    }
}
