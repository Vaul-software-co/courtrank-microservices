package com.example.userService.application.useCases;

import com.example.userService.application.dto.RestoreUserRequest;
import com.example.userService.application.ports.audit.UserAuditEvent;
import com.example.userService.application.ports.audit.UserAuditEventType;
import com.example.userService.application.ports.audit.UserAuditLogger;
import com.example.userService.domain.entity.User;
import com.example.userService.domain.enums.UserProfileStatus;
import com.example.userService.domain.exceptions.UserNameAlreadyTakenException;
import com.example.userService.domain.repository.UserRepository;

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
            User user = User.create(request.id(), request.name(), request.userName(), request.email());
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
