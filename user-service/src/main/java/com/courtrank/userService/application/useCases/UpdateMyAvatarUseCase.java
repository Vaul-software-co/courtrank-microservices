package com.courtrank.userService.application.useCases;

import com.courtrank.userService.application.dto.TraceContext;
import com.courtrank.userService.application.dto.UpdateMyAvatarRequest;
import com.courtrank.userService.application.dto.UpdateMyAvatarResponse;
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
import java.util.Objects;

public class UpdateMyAvatarUseCase {
    private final UserRepository userRepository;
    private final UserAuditLogger auditLogger;
    private final UserEventPublisher eventPublisher;

    public UpdateMyAvatarUseCase(UserRepository userRepository, UserAuditLogger auditLogger) {
        this(userRepository, auditLogger, new NoOpUserEventPublisher());
    }

    public UpdateMyAvatarUseCase(UserRepository userRepository, UserAuditLogger auditLogger, UserEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
        this.eventPublisher = eventPublisher;
    }

    public UpdateMyAvatarResponse execute(UpdateMyAvatarRequest request, TraceContext trace) {
        User user = this.userRepository.findById(request.userId())
                .orElse(null);

        if (user == null) {
            this.auditLogger.log(new UserAuditEvent(
                    UserAuditEventType.USER_PROFILE_AVATAR_UPDATE_FAILED_NOT_FOUND,
                    request.userId(),
                    request.userId(),
                    TraceContext.traceIdOrNull(trace),
                    Map.of(),
                    Instant.now()
            ));
            throw new InvalidCredentialsException();
        }

        String previousAvatarUrl = user.getAvatarUrl();
        if (Objects.equals(previousAvatarUrl, request.avatarKey())) {
            return new UpdateMyAvatarResponse(request.avatarKey(), user.getAvatarUrl());
        }

        user.changeAvatarUrl(request.avatarKey());
        this.userRepository.save(user);

        this.auditLogger.log(new UserAuditEvent(
                UserAuditEventType.USER_PROFILE_AVATAR_UPDATED,
                user.getId(),
                user.getId(),
                TraceContext.traceIdOrNull(trace),
                Map.of(
                        "previousAvatarUrl", String.valueOf(previousAvatarUrl),
                        "avatarKey", request.avatarKey()
                ),
                Instant.now()
        ));
        if (this.eventPublisher != null) {
            this.eventPublisher.publishUserProfileUpdated(this.toEvent(user));
        }

        return new UpdateMyAvatarResponse(request.avatarKey(), user.getAvatarUrl());
    }

    private UserProfileChangedEvent toEvent(User user) {
        return new UserProfileChangedEvent(user.getId(), user.getName(), user.getUserName(), user.getAvatarUrl(), user.isPrivateProfile(), user.getStatus(), Instant.now());
    }
}
