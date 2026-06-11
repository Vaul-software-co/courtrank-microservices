package com.courtrank.userService.application.useCases;

import com.courtrank.userService.application.dto.CreateUserRequest;
import com.courtrank.userService.application.events.UserProfileCreatedEvent;
import com.courtrank.userService.application.ports.UserEventPublisher;
import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditEventType;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.repository.UserRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public class CreateUserFromAuthEventUseCase {

    private final UserRepository userRepository;
    private final UserAuditLogger auditLogger;
    private final UserEventPublisher eventPublisher;

    public CreateUserFromAuthEventUseCase(
            UserRepository userRepository,
            UserAuditLogger auditLogger,
            UserEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
        this.eventPublisher = eventPublisher;
    }

    public void execute(CreateUserRequest request){
        Optional<User> existingUser = this.userRepository.findById(request.id());

        if (existingUser.isPresent()) {
            this.auditLogger.log(new UserAuditEvent(
                    UserAuditEventType.USER_PROFILE_CREATION_SKIPPED_ALREADY_EXISTS,
                    null,
                    request.id(),
                    null,
                    Map.of(
                            "email", request.email(),
                            "username", String.valueOf(request.userName())
                    ),
                    Instant.now()
            ));
            return;
        }

        String username = request.userName();

        if (username != null && !username.isBlank()) {
            Optional<User> usernameTaken = this.userRepository.findByUsername(request.userName());

            if(usernameTaken.isPresent()) {
                this.auditLogger.log(new UserAuditEvent(
                        UserAuditEventType.USER_PROFILE_CREATION_FAILED_USERNAME_CONFLICT,
                        null,
                        request.id(),
                        null,
                        Map.of(
                                "email", request.email(),
                                "username", request.userName(),
                                "conflictingUserId", usernameTaken.orElseThrow().getId().toString()
                        ),
                        Instant.now()
                ));
                username = null;
            }
        }

        User newUser = User.create(request.id(), request.name(), username, request.email(), request.emailVerified());

        this.userRepository.save(newUser);
        this.auditLogger.log(new UserAuditEvent(
                UserAuditEventType.USER_PROFILE_CREATED_FROM_AUTH_EVENT,
                null,
                newUser.getId(),
                null,
                Map.of(
                        "email", newUser.getEmail(),
                        "username", String.valueOf(newUser.getUserName())
                ),
                Instant.now()
        ));
        this.eventPublisher.publishUserProfileCreated(new UserProfileCreatedEvent(
                newUser.getId(),
                newUser.getEmail(),
                newUser.getName(),
                newUser.getUserName(),
                newUser.isPrivateProfile(),
                newUser.getStatus(),
                Instant.now()
        ));
    }
}
