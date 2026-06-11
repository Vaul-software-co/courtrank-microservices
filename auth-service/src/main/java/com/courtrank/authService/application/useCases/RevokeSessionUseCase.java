package com.courtrank.authService.application.useCases;

import com.courtrank.authService.application.dto.RevokeSessionRequest;
import com.courtrank.authService.application.ports.audit.AuditEvent;
import com.courtrank.authService.application.ports.audit.AuditEventType;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.entity.Session;
import com.courtrank.authService.domain.exceptions.ForbiddenException;
import com.courtrank.authService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.domain.repository.SessionRepository;

import java.time.Instant;
import java.util.Map;

public class RevokeSessionUseCase {
    private final AuthenticationRepository authenticationRepository;
    private final SessionRepository sessionRepository;
    private final AuditLogger auditLogger;

    public RevokeSessionUseCase(
            AuthenticationRepository authenticationRepository,
            SessionRepository sessionRepository,
            AuditLogger auditLogger
    ) {
        this.authenticationRepository = authenticationRepository;
        this.sessionRepository = sessionRepository;
        this.auditLogger = auditLogger;
    }

    public void execute(RevokeSessionRequest request) {
        Authentication auth = this.authenticationRepository.findById(request.userId())
                .orElseThrow(InvalidCredentialsException::new);

        if (auth.isDeleted()) throw new InvalidCredentialsException();
        if (!auth.isActive()) throw new InvalidCredentialsException();

        Session session = this.sessionRepository.findById(request.sessionId())
                .orElseThrow(InvalidCredentialsException::new);

        if (!session.getUserId().equals(auth.getId())) {
            throw new ForbiddenException();
        }

        if (!session.isActive()) return;

        session.revoke();
        this.sessionRepository.save(session);
        this.auditLogger.log(new AuditEvent(
                AuditEventType.AUTH_SESSION_REVOKED,
                auth.getId(),
                auth.getId(),
                null,
                Map.of("sessionId", session.getId().toString()),
                Instant.now()
        ));
    }
}
