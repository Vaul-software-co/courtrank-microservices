package com.example.authService.application.useCases;

import com.example.authService.application.dto.RevokeAllSessionsRequest;
import com.example.authService.application.ports.audit.AuditEvent;
import com.example.authService.application.ports.audit.AuditEventType;
import com.example.authService.application.ports.audit.AuditLogger;
import com.example.authService.domain.entity.Authentication;
import com.example.authService.domain.exceptions.InvalidCredentialsException;
import com.example.authService.domain.repository.AuthenticationRepository;
import com.example.authService.domain.repository.SessionRepository;

import java.time.Instant;
import java.util.Map;

public class RevokeAllSessionsUseCase {
    private final AuthenticationRepository authenticationRepository;
    private final SessionRepository sessionRepository;
    private final AuditLogger auditLogger;

    public RevokeAllSessionsUseCase(
            AuthenticationRepository authenticationRepository,
            SessionRepository sessionRepository,
            AuditLogger auditLogger
    ) {
        this.authenticationRepository = authenticationRepository;
        this.sessionRepository = sessionRepository;
        this.auditLogger = auditLogger;
    }

    public void execute(RevokeAllSessionsRequest request) {
        Authentication auth = this.authenticationRepository.findById(request.userId())
                .orElseThrow(InvalidCredentialsException::new);

        if (auth.isDeleted()) throw new InvalidCredentialsException();
        if (!auth.isActive()) throw new InvalidCredentialsException();

        this.sessionRepository.revokeAllByUserId(auth.getId());
        this.auditLogger.log(new AuditEvent(
                AuditEventType.AUTH_ALL_SESSIONS_REVOKED,
                auth.getId(),
                auth.getId(),
                null,
                Map.of(),
                Instant.now()
        ));
    }
}
