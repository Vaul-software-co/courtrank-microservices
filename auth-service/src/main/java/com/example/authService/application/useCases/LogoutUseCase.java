package com.example.authService.application.useCases;

import com.example.authService.application.dto.LogoutRequest;
import com.example.authService.application.ports.audit.AuditEvent;
import com.example.authService.application.ports.audit.AuditEventType;
import com.example.authService.application.ports.audit.AuditLogger;
import com.example.authService.application.ports.security.TokenHasher;
import com.example.authService.domain.entity.Session;
import com.example.authService.domain.exceptions.InvalidCredentialsException;
import com.example.authService.domain.repository.SessionRepository;

import java.time.Instant;
import java.util.Map;

public class LogoutUseCase {
    private final SessionRepository sessionRepository;
    private final TokenHasher tokenHasher;
    private final AuditLogger auditLogger;

    public LogoutUseCase(
            SessionRepository sessionRepository,
            TokenHasher tokenHasher,
            AuditLogger auditLogger
    ) {
        this.sessionRepository = sessionRepository;
        this.tokenHasher = tokenHasher;
        this.auditLogger = auditLogger;
    }

    public void execute(LogoutRequest request) {
        String refreshTokenHash = this.tokenHasher.hash(request.refreshToken());
        Session session = this.sessionRepository.findByTokenHash(refreshTokenHash)
                .orElseThrow(InvalidCredentialsException::new);

        if (!session.isActive()) return;

        session.revoke();
        this.sessionRepository.save(session);
        this.auditLogger.log(new AuditEvent(
                AuditEventType.AUTH_SESSION_REVOKED,
                session.getUserId(),
                session.getUserId(),
                null,
                Map.of("sessionId", session.getId().toString()),
                Instant.now()
        ));
    }
}
