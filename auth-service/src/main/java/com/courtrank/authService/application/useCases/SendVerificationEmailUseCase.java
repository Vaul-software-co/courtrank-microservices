package com.courtrank.authService.application.useCases;

import com.courtrank.authService.application.dto.VerificationEmailRequest;
import com.courtrank.authService.application.ports.audit.AuditEvent;
import com.courtrank.authService.application.ports.audit.AuditEventType;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.ports.email.EmailSender;
import com.courtrank.authService.application.ports.security.VerificationTokenGenerator;
import com.courtrank.authService.domain.entity.VerificationToken;
import com.courtrank.authService.domain.enums.VerificationTokenType;
import com.courtrank.authService.domain.repository.VerificationTokenRepository;

import java.time.Instant;
import java.util.Map;

public class SendVerificationEmailUseCase {
    private final VerificationTokenGenerator tokenGenerator;
    private final VerificationTokenRepository verificationTokenRepository;
    private final String frontendUrl;
    private final EmailSender emailSender;
    private final AuditLogger auditLogger;

    public SendVerificationEmailUseCase(
            VerificationTokenGenerator tokenGenerator,
            VerificationTokenRepository verificationTokenRepository,
            String frontendUrl,
            EmailSender emailSender,
            AuditLogger auditLogger
    ) {
        this.tokenGenerator = tokenGenerator;
        this.verificationTokenRepository = verificationTokenRepository;
        this.frontendUrl = frontendUrl;
        this.emailSender = emailSender;
        this.auditLogger = auditLogger;
    }

    public void execute(VerificationEmailRequest request) {
        String rawToken = this.tokenGenerator.generateUrlToken();
        String tokenHash = this.tokenGenerator.hash(rawToken);

        this.verificationTokenRepository.invalidatePrevious(request.id(), VerificationTokenType.EMAIL_VERIFICATION);
        VerificationToken token = VerificationToken.create(request.id(), tokenHash, VerificationTokenType.EMAIL_VERIFICATION);

        this.verificationTokenRepository.save(token);

        String link = this.frontendUrl + "/verify-email?token=" + rawToken + "&userId=" + request.id();

        this.emailSender.sendEmailVerification(request.email(), link, request.lang());
        this.auditLogger.log(new AuditEvent(
                AuditEventType.AUTH_VERIFICATION_EMAIL_SENT,
                request.id(),
                request.id(),
                null,
                Map.of("lang", String.valueOf(request.lang())),
                Instant.now()
        ));
    }
}
