package com.example.userService.application.useCases;

import com.example.userService.application.dto.RemoveMyAvatarRequest;
import com.example.userService.application.dto.RemoveMyAvatarResponse;
import com.example.userService.application.dto.TraceContext;
import com.example.userService.application.ports.audit.UserAuditEvent;
import com.example.userService.application.ports.audit.UserAuditEventType;
import com.example.userService.application.ports.audit.UserAuditLogger;
import com.example.userService.domain.entity.User;
import com.example.userService.domain.exceptions.InvalidCredentialsException;
import com.example.userService.domain.repository.UserRepository;

import java.time.Instant;
import java.util.Map;

public class RemoveMyAvatarUseCase {
    private final UserRepository userRepository;
    private final UserAuditLogger auditLogger;

    public RemoveMyAvatarUseCase(UserRepository userRepository, UserAuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
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

        return new RemoveMyAvatarResponse(null, null);
    }
}
