package com.example.authService.application.useCases;

import com.example.authService.application.dto.ListSessionsRequest;
import com.example.authService.application.dto.SessionSummary;
import com.example.authService.application.ports.audit.AuditEvent;
import com.example.authService.application.ports.audit.AuditEventType;
import com.example.authService.application.ports.audit.AuditLogger;
import com.example.authService.domain.entity.Authentication;
import com.example.authService.domain.entity.Session;
import com.example.authService.domain.exceptions.InvalidCredentialsException;
import com.example.authService.domain.repository.AuthenticationRepository;
import com.example.authService.domain.repository.SessionRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ListSessionsUseCase {
    private final AuthenticationRepository authenticationRepository;
    private final SessionRepository sessionRepository;
    private final AuditLogger auditLogger;

    public ListSessionsUseCase(
            AuthenticationRepository authenticationRepository,
            SessionRepository sessionRepository,
            AuditLogger auditLogger
    ) {
        this.authenticationRepository = authenticationRepository;
        this.sessionRepository = sessionRepository;
        this.auditLogger = auditLogger;
    }

    public List<SessionSummary> execute(ListSessionsRequest request) {
        Authentication auth = this.authenticationRepository.findById(request.userId())
                .orElseThrow(InvalidCredentialsException::new);

        if (auth.isDeleted()) throw new InvalidCredentialsException();
        if (!auth.isActive()) throw new InvalidCredentialsException();

        List<SessionSummary> sessions = this.sessionRepository.findActiveByUserId(auth.getId())
                .stream()
                .filter(Session::isActive)
                .map(session -> new SessionSummary(
                        session.getId(),
                        session.getClient(),
                        session.getIp(),
                        session.getUserAgent(),
                        session.getCreatedAt(),
                        session.getExpiresAt()
                ))
                .toList();

        this.auditLogger.log(new AuditEvent(
                AuditEventType.AUTH_SESSIONS_LISTED,
                auth.getId(),
                auth.getId(),
                null,
                Map.of("count", sessions.size()),
                Instant.now()
        ));

        return sessions;
    }
}
