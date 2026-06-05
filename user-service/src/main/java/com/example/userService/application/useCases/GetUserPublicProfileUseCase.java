package com.example.userService.application.useCases;

import com.example.userService.application.dto.GetPublicProfileRequest;
import com.example.userService.application.dto.PublicProfileResponse;
import com.example.userService.application.dto.TraceContext;
import com.example.userService.application.ports.audit.UserAuditEvent;
import com.example.userService.application.ports.audit.UserAuditEventType;
import com.example.userService.application.ports.audit.UserAuditLogger;
import com.example.userService.domain.entity.User;
import com.example.userService.domain.enums.UserProfileStatus;
import com.example.userService.domain.exceptions.UserProfileNotFoundException;
import com.example.userService.domain.repository.UserRepository;

import java.time.Instant;
import java.util.Map;

public class GetUserPublicProfileUseCase {
    private final UserRepository userRepository;
    private final UserAuditLogger auditLogger;

    public GetUserPublicProfileUseCase(UserRepository userRepository, UserAuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
    }

    public PublicProfileResponse execute(GetPublicProfileRequest request, TraceContext trace) {
        User user = this.userRepository.findById(request.userId())
                .orElse(null);

        if (user == null || user.getStatus() == UserProfileStatus.DELETED || user.getStatus() == UserProfileStatus.SUSPENDED) {
            this.auditLogger.log(new UserAuditEvent(
                    UserAuditEventType.USER_PROFILE_PUBLIC_LOOKUP_FAILED_NOT_FOUND,
                    null,
                    request.userId(),
                    TraceContext.traceIdOrNull(trace),
                    Map.of(),
                    Instant.now()
            ));
            throw new UserProfileNotFoundException();
        }

        return new PublicProfileResponse(
                user.getId(),
                user.getName(),
                user.getUserName(),
                user.getAvatarUrl(),
                user.isPrivateProfile(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
