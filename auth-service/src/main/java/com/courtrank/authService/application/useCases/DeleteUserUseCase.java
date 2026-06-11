package com.courtrank.authService.application.useCases;

import com.courtrank.authService.application.dto.DeleteUserRequest;
import com.courtrank.authService.application.events.UserDeletedEvent;
import com.courtrank.authService.application.ports.AuthEventPublisher;
import com.courtrank.authService.application.ports.audit.AuditEvent;
import com.courtrank.authService.application.ports.audit.AuditEventType;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.domain.repository.SessionRepository;

import java.time.Instant;
import java.util.Map;

public class DeleteUserUseCase {

    private final AuthenticationRepository authenticationRepository;
    private final SessionRepository sessionRepository;
    private final AuthEventPublisher eventPublisher;
    private final AuditLogger auditLogger;

    public  DeleteUserUseCase(
            AuthenticationRepository authenticationRepository,
            SessionRepository sessionRepository,
            AuthEventPublisher eventPublisher,
            AuditLogger auditLogger
    ){
        this.authenticationRepository = authenticationRepository;
        this.sessionRepository = sessionRepository;
        this.eventPublisher = eventPublisher;
        this.auditLogger = auditLogger;
    }

    public void execute(DeleteUserRequest request) {
        Authentication auth = this.authenticationRepository.findById(request.userId())
                .orElseThrow(InvalidCredentialsException::new);

        if (auth.isDeleted()) return;

        auth.deleteUser();

        this.authenticationRepository.save(auth);
        this.sessionRepository.revokeAllByUserId(auth.getId());
        this.eventPublisher.publishUserDeleted(new UserDeletedEvent(
                auth.getId(),
                auth.getEmail(),
                Instant.now()
        ));
        this.auditLogger.log(new AuditEvent(
                AuditEventType.AUTH_USER_DELETED,
                auth.getId(),
                auth.getId(),
                null,
                Map.of("sessionsRevoked", true),
                Instant.now()
        ));
    }
}
