package com.courtrank.userService.application.useCases;

import com.courtrank.userService.application.dto.TraceContext;
import com.courtrank.userService.application.dto.UnbanUserProfileRequest;
import com.courtrank.userService.application.dto.UserProfileStatusResponse;
import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditEventType;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.enums.UserProfileStatus;
import com.courtrank.userService.domain.exceptions.UserProfileNotFoundException;
import com.courtrank.userService.domain.repository.UserRepository;

import java.time.Instant;
import java.util.Map;

public class UnbanUserProfileUseCase {
    private final UserRepository userRepository;
    private final UserAuditLogger auditLogger;

    public UnbanUserProfileUseCase(UserRepository userRepository, UserAuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
    }

    public UserProfileStatusResponse execute(UnbanUserProfileRequest request, TraceContext trace) {
        User user = this.userRepository.findById(request.targetUserId())
                .orElse(null);

        if (user == null || user.getStatus() == UserProfileStatus.DELETED) {
            this.auditLogger.log(new UserAuditEvent(
                    UserAuditEventType.USER_PROFILE_UNBAN_FAILED_NOT_FOUND,
                    request.adminUserId(),
                    request.targetUserId(),
                    TraceContext.traceIdOrNull(trace),
                    Map.of(),
                    Instant.now()
            ));
            throw new UserProfileNotFoundException();
        }

        if (user.getStatus() != UserProfileStatus.SUSPENDED) {
            return new UserProfileStatusResponse(user.getStatus());
        }

        UserProfileStatus previousStatus = user.getStatus();
        user.showProfile();
        this.userRepository.save(user);
        this.auditLogger.log(new UserAuditEvent(
                UserAuditEventType.USER_PROFILE_UNBANNED,
                request.adminUserId(),
                user.getId(),
                TraceContext.traceIdOrNull(trace),
                Map.of(
                        "previousStatus", previousStatus.name(),
                        "status", user.getStatus().name()
                ),
                Instant.now()
        ));

        return new UserProfileStatusResponse(user.getStatus());
    }
}
