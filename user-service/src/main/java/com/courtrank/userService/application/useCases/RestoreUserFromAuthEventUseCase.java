package com.courtrank.userService.application.useCases;

import com.courtrank.userService.application.dto.RestoreUserRequest;
import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditEventType;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.enums.UserProfileStatus;
import com.courtrank.userService.domain.exceptions.UserNameAlreadyTakenException;
import com.courtrank.userService.domain.repository.UserRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public class RestoreUserFromAuthEventUseCase {
    private final UserRepository userRepository;
    private final UserAuditLogger auditLogger;

    public RestoreUserFromAuthEventUseCase(UserRepository userRepository, UserAuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
    }

    public void execute(RestoreUserRequest request) {
        User existingUser = this.userRepository.findById(request.id())
                .orElse(null);

        if (existingUser == null) {
            this.assertUsernameAvailable(request);
            User user = User.create(request.id(), request.name(), request.userName(), request.email(), request.emailVerified());
            this.userRepository.save(user);
            this.auditLogger.log(new UserAuditEvent(
                    UserAuditEventType.USER_PROFILE_RESTORE_CREATED_FROM_AUTH_EVENT,
                    null,
                    user.getId(),
                    null,
                    Map.of(
                            "email", user.getEmail(),
                            "username", String.valueOf(user.getUserName())
                    ),
                    Instant.now()
            ));
            return;
        }

        if (existingUser.getStatus() != UserProfileStatus.DELETED) {
            this.auditLogger.log(new UserAuditEvent(
                    UserAuditEventType.USER_PROFILE_RESTORE_SKIPPED_ALREADY_ACTIVE,
                    null,
                    existingUser.getId(),
                    null,
                    Map.of("status", existingUser.getStatus().name()),
                    Instant.now()
            ));
            return;
        }

        existingUser.showProfile();
        if (request.emailVerified()) {
            existingUser.markEmailVerified();
        }
        this.userRepository.save(existingUser);
        this.auditLogger.log(new UserAuditEvent(
                UserAuditEventType.USER_PROFILE_RESTORED_FROM_AUTH_EVENT,
                null,
                existingUser.getId(),
                null,
                Map.of(
                        "email", existingUser.getEmail(),
                        "username", String.valueOf(existingUser.getUserName())
                ),
                Instant.now()
        ));
    }

    private void assertUsernameAvailable(RestoreUserRequest request) {
        if (request.userName() == null || request.userName().isBlank()) {
            return;
        }

        Optional<User> usernameOwner = this.userRepository.findByUsername(request.userName());
        if (usernameOwner.isPresent()) {
            throw new UserNameAlreadyTakenException();
        }
    }
}
