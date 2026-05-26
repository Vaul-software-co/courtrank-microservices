package com.example.authService.application.useCases;

import com.example.authService.application.dto.DeleteUserRequest;
import com.example.authService.application.ports.audit.AuditEvent;
import com.example.authService.application.ports.audit.AuditEventType;
import com.example.authService.application.ports.audit.AuditLogger;
import com.example.authService.domain.entity.Authentication;
import com.example.authService.domain.exceptions.InvalidCredentialsException;
import com.example.authService.domain.repository.AuthenticationRepository;
import com.example.authService.domain.repository.SessionRepository;

import java.time.Instant;
import java.util.Map;

public class DeleteUserUseCase {

    private final AuthenticationRepository authenticationRepository;
    private final SessionRepository sessionRepository;
    private final AuditLogger auditLogger;

    public  DeleteUserUseCase(AuthenticationRepository authenticationRepository, SessionRepository sessionRepository, AuditLogger auditLogger){
        this.authenticationRepository = authenticationRepository;
        this.sessionRepository = sessionRepository;
        this.auditLogger = auditLogger;
    }

    public void execute(DeleteUserRequest request) {
        Authentication auth = this.authenticationRepository.findById(request.userId())
                .orElseThrow(InvalidCredentialsException::new);

        if (auth.isDeleted()) return;

        auth.deleteUser();

        this.authenticationRepository.save(auth);
        this.sessionRepository.revokeAllByUserId(auth.getId());
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
