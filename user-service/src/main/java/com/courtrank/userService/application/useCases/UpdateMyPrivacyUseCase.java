package com.courtrank.userService.application.useCases;

import com.courtrank.userService.application.dto.TraceContext;
import com.courtrank.userService.application.dto.UpdateMyPrivacyRequest;
import com.courtrank.userService.application.dto.UpdateMyPrivacyResponse;
import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditEventType;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.userService.domain.repository.UserRepository;

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
