package com.example.userService.application.useCases;

import com.example.userService.application.dto.TraceContext;
import com.example.userService.application.dto.UpdateMyPrivacyRequest;
import com.example.userService.application.dto.UpdateMyPrivacyResponse;
import com.example.userService.application.ports.audit.UserAuditEvent;
import com.example.userService.application.ports.audit.UserAuditEventType;
import com.example.userService.application.ports.audit.UserAuditLogger;
import com.example.userService.domain.entity.User;
import com.example.userService.domain.exceptions.InvalidCredentialsException;
import com.example.userService.domain.repository.UserRepository;

import java.time.Instant;
import java.util.Map;

public class UpdateMyPrivacyUseCase {
    private final UserRepository userRepository;
    private final UserAuditLogger auditLogger;

    public UpdateMyPrivacyUseCase(UserRepository userRepository, UserAuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
    }

    public UpdateMyPrivacyResponse execute(UpdateMyPrivacyRequest request, TraceContext trace) {
        User user = this.userRepository.findById(request.userId())
                .orElse(null);

        if (user == null) {
            this.auditLogger.log(new UserAuditEvent(
                    UserAuditEventType.USER_PROFILE_PRIVACY_UPDATE_FAILED_NOT_FOUND,
                    request.userId(),
                    request.userId(),
                    TraceContext.traceIdOrNull(trace),
                    Map.of(),
                    Instant.now()
            ));
            throw new InvalidCredentialsException();
        }

        boolean previousPrivateProfile = user.isPrivateProfile();
        if (previousPrivateProfile == request.privateProfile()) {
            return new UpdateMyPrivacyResponse(user.isPrivateProfile());
        }

        user.changePrivacy(request.privateProfile());
        this.userRepository.save(user);

        this.auditLogger.log(new UserAuditEvent(
                UserAuditEventType.USER_PROFILE_PRIVACY_UPDATED,
                user.getId(),
                user.getId(),
                TraceContext.traceIdOrNull(trace),
                Map.of(
                        "previousPrivateProfile", previousPrivateProfile,
                        "privateProfile", user.isPrivateProfile()
                ),
                Instant.now()
        ));

        return new UpdateMyPrivacyResponse(user.isPrivateProfile());
    }
}
