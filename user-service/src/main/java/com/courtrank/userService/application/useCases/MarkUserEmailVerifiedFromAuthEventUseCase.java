package com.courtrank.userService.application.useCases;

import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditEventType;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.repository.UserRepository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class MarkUserEmailVerifiedFromAuthEventUseCase {
    private final UserRepository userRepository;
    private final UserAuditLogger auditLogger;

    public MarkUserEmailVerifiedFromAuthEventUseCase(UserRepository userRepository, UserAuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
    }

    public void execute(UUID userId) {
        User user = this.userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        if (user.isEmailVerified()) {
            return;
        }

        user.markEmailVerified();
        this.userRepository.save(user);
        this.auditLogger.log(new UserAuditEvent(
                UserAuditEventType.USER_PROFILE_UPDATED,
                null,
                user.getId(),
                null,
                Map.of("emailVerified", true),
                Instant.now()
        ));
    }
}
