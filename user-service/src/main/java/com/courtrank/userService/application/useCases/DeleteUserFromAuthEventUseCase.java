package com.courtrank.userService.application.useCases;

import com.courtrank.userService.application.events.UserProfileDeletedEvent;
import com.courtrank.userService.application.ports.NoOpUserEventPublisher;
import com.courtrank.userService.application.ports.UserEventPublisher;
import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditEventType;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.enums.UserProfileStatus;
import com.courtrank.userService.domain.repository.UserRepository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class DeleteUserFromAuthEventUseCase {
    private final UserRepository userRepository;
    private final UserAuditLogger auditLogger;
    private final UserEventPublisher eventPublisher;

    public DeleteUserFromAuthEventUseCase(UserRepository userRepository, UserAuditLogger auditLogger) {
        this(userRepository, auditLogger, new NoOpUserEventPublisher());
    }

    public DeleteUserFromAuthEventUseCase(UserRepository userRepository, UserAuditLogger auditLogger, UserEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
        this.eventPublisher = eventPublisher;
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
        if (this.eventPublisher != null) {
            this.eventPublisher.publishUserProfileDeleted(new UserProfileDeletedEvent(user.getId(), Instant.now()));
        }
    }
}
