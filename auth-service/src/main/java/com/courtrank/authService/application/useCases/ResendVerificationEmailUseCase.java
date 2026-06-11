package com.courtrank.authService.application.useCases;

import com.courtrank.authService.application.dto.ResendVerificationEmailRequest;
import com.courtrank.authService.application.dto.VerificationEmailRequest;
import com.courtrank.authService.application.ports.audit.AuditEvent;
import com.courtrank.authService.application.ports.audit.AuditEventType;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.domain.repository.AuthenticationRepository;

import java.time.Instant;
import java.util.Map;

public class ResendVerificationEmailUseCase {
    private final AuthenticationRepository authenticationRepository;
    private final SendVerificationEmailUseCase sendEmail;
    private final AuditLogger auditLogger;

    public ResendVerificationEmailUseCase(
            AuthenticationRepository authenticationRepository,
            SendVerificationEmailUseCase sendEmail,
            AuditLogger auditLogger
    ) {
        this.authenticationRepository = authenticationRepository;
        this.sendEmail = sendEmail;
        this.auditLogger = auditLogger;
    }

    public void execute(ResendVerificationEmailRequest request) {
        this.authenticationRepository.findByEmail(request.email())
                .filter(auth -> !auth.isEmailVerified())
                .ifPresent(auth -> {
                    this.sendEmail.execute(
                            new VerificationEmailRequest(
                                    auth.getId(),
                                    auth.getEmail(),
                                    request.lang()
                            )
                    );
                    this.auditLogger.log(new AuditEvent(
                            AuditEventType.AUTH_VERIFICATION_EMAIL_RESENT,
                            auth.getId(),
                            auth.getId(),
                            null,
                            Map.of("lang", String.valueOf(request.lang())),
                            Instant.now()
                    ));
                });
    }
}
