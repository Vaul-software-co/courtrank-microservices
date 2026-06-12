package com.courtrank.userService.application.useCases;

import com.courtrank.userService.application.dto.BanUserProfileRequest;
import com.courtrank.userService.application.dto.TraceContext;
import com.courtrank.userService.application.dto.UserProfileStatusResponse;
import com.courtrank.userService.application.events.UserProfileChangedEvent;
import com.courtrank.userService.application.ports.NoOpUserEventPublisher;
import com.courtrank.userService.application.ports.UserEventPublisher;
import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditEventType;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.enums.UserProfileStatus;
import com.courtrank.userService.domain.exceptions.UserProfileNotFoundException;
import com.courtrank.userService.domain.repository.UserRepository;

import java.time.Instant;
import java.util.Map;

public class BanUserProfileUseCase {
    private final UserRepository userRepository;
    private final UserAuditLogger auditLogger;
    private final UserEventPublisher eventPublisher;

    public BanUserProfileUseCase(UserRepository userRepository, UserAuditLogger auditLogger) {
        this(userRepository, auditLogger, new NoOpUserEventPublisher());
    }

    public BanUserProfileUseCase(UserRepository userRepository, UserAuditLogger auditLogger, UserEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
        this.eventPublisher = eventPublisher;
    }

    public UserProfileStatusResponse execute(BanUserProfileRequest request, TraceContext trace) {
        User user = this.userRepository.findById(request.targetUserId())
                .orElse(null);

        if (user == null || user.getStatus() == UserProfileStatus.DELETED) {
            this.auditLogger.log(new UserAuditEvent(
                    UserAuditEventType.USER_PROFILE_BAN_FAILED_NOT_FOUND,
                    request.adminUserId(),
                    request.targetUserId(),
                    TraceContext.traceIdOrNull(trace),
                    Map.of(),
                    Instant.now()
            ));
            throw new UserProfileNotFoundException();
        }

        if (user.getStatus() == UserProfileStatus.SUSPENDED) {
            return new UserProfileStatusResponse(user.getStatus());
        }

        UserProfileStatus previousStatus = user.getStatus();
        user.suspendProfile();
        this.userRepository.save(user);
        this.auditLogger.log(new UserAuditEvent(
                UserAuditEventType.USER_PROFILE_BANNED,
                request.adminUserId(),
                user.getId(),
                TraceContext.traceIdOrNull(trace),
                Map.of(
                        "previousStatus", previousStatus.name(),
                        "status", user.getStatus().name()
                ),
                Instant.now()
        ));
        if (this.eventPublisher != null) {
            this.eventPublisher.publishUserProfileUpdated(this.toEvent(user));
        }

        return new UserProfileStatusResponse(user.getStatus());
    }

    private UserProfileChangedEvent toEvent(User user) {
        return new UserProfileChangedEvent(user.getId(), user.getName(), user.getUserName(), user.getAvatarUrl(), user.isPrivateProfile(), user.getStatus(), Instant.now());
    }
}
