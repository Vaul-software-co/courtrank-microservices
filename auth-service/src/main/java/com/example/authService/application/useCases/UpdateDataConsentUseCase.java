package com.example.authService.application.useCases;

import com.example.authService.application.dto.UpdateDataConsentRequest;
import com.example.authService.application.dto.UpdateDataConsentResponse;
import com.example.authService.application.ports.audit.AuditEvent;
import com.example.authService.application.ports.audit.AuditEventType;
import com.example.authService.application.ports.audit.AuditLogger;
import com.example.authService.domain.entity.Authentication;
import com.example.authService.domain.exceptions.InvalidCredentialsException;
import com.example.authService.domain.repository.AuthenticationRepository;

import java.time.Instant;
import java.util.Map;

public class UpdateDataConsentUseCase {
    private final AuthenticationRepository authenticationRepository;
    private final AuditLogger auditLogger;

    public UpdateDataConsentUseCase(AuthenticationRepository authenticationRepository, AuditLogger auditLogger) {
        this.authenticationRepository = authenticationRepository;
        this.auditLogger = auditLogger;
    }

    public UpdateDataConsentResponse execute(UpdateDataConsentRequest request) {
        Authentication auth = this.authenticationRepository.findById(request.userId())
                .orElseThrow(InvalidCredentialsException::new);

        if (auth.isDeleted() || !auth.isActive()) {
            throw new InvalidCredentialsException();
        }

        auth.updateDataConsent(request.accept());
        this.authenticationRepository.save(auth);
        this.auditLogger.log(new AuditEvent(
                AuditEventType.AUTH_DATA_CONSENT_UPDATED,
                auth.getId(),
                auth.getId(),
                null,
                Map.of("accepted", request.accept()),
                Instant.now()
        ));

        return new UpdateDataConsentResponse(auth.getDataConsentAcceptedAt());
    }
}
