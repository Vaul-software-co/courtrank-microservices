package com.courtrank.userService.application.useCases;

import com.courtrank.userService.application.dto.RemoveMyAvatarRequest;
import com.courtrank.userService.application.dto.RemoveMyAvatarResponse;
import com.courtrank.userService.application.dto.TraceContext;
import com.courtrank.userService.application.events.UserProfileChangedEvent;
import com.courtrank.userService.application.ports.NoOpUserEventPublisher;
import com.courtrank.userService.application.ports.UserEventPublisher;
import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditEventType;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.userService.domain.repository.UserRepository;

import java.time.Instant;
import java.util.Map;

public class RemoveMyAvatarUseCase {
    private final UserRepository userRepository;
    private final UserAuditLogger auditLogger;
    private final UserEventPublisher eventPublisher;

    public RemoveMyAvatarUseCase(UserRepository userRepository, UserAuditLogger auditLogger) {
        this(userRepository, auditLogger, new NoOpUserEventPublisher());
    }

    public RemoveMyAvatarUseCase(UserRepository userRepository, UserAuditLogger auditLogger, UserEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
        this.eventPublisher = eventPublisher;
    }

    public RemoveMyAvatarResponse execute(RemoveMyAvatarRequest request, TraceContext trace) {
        User user = this.userRepository.findById(request.userId())
                .orElse(null);

        if (user == null) {
            this.auditLogger.log(new UserAuditEvent(
                    UserAuditEventType.USER_PROFILE_AVATAR_REMOVE_FAILED_NOT_FOUND,
                    request.userId(),
                    request.userId(),
                    TraceContext.traceIdOrNull(trace),
                    Map.of(),
                    Instant.now()
            ));
            throw new InvalidCredentialsException();
        }

        String previousAvatarUrl = user.getAvatarUrl();
        if (previousAvatarUrl == null) {
            return new RemoveMyAvatarResponse(null, null);
        }

        user.changeAvatarUrl(null);
        this.userRepository.save(user);

        this.auditLogger.log(new UserAuditEvent(
                UserAuditEventType.USER_PROFILE_AVATAR_REMOVED,
                user.getId(),
                user.getId(),
                TraceContext.traceIdOrNull(trace),
                Map.of("previousAvatarUrl", previousAvatarUrl),
                Instant.now()
        ));
        if (this.eventPublisher != null) {
            this.eventPublisher.publishUserProfileUpdated(this.toEvent(user));
        }

        return new RemoveMyAvatarResponse(null, null);
    }

    private UserProfileChangedEvent toEvent(User user) {
        return new UserProfileChangedEvent(user.getId(), user.getName(), user.getUserName(), user.getAvatarUrl(), user.isPrivateProfile(), user.getStatus(), Instant.now());
    }
}
