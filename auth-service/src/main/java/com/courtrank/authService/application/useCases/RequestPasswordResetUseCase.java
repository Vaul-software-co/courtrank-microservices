package com.courtrank.authService.application.useCases;

import com.courtrank.authService.application.dto.RequestPasswordResetRequest;
import com.courtrank.authService.application.ports.email.EmailSender;
import com.courtrank.authService.application.ports.audit.AuditEvent;
import com.courtrank.authService.application.ports.audit.AuditEventType;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.ports.security.VerificationTokenGenerator;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.entity.VerificationToken;
import com.courtrank.authService.domain.enums.VerificationTokenType;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.domain.repository.VerificationTokenRepository;

import java.time.Instant;
import java.util.Map;

public class RequestPasswordResetUseCase {
    private final VerificationTokenRepository tokenRepository;
    private final AuthenticationRepository authenticationRepository;
    private final VerificationTokenGenerator tokenGenerator;
    private final EmailSender emailSender;
    private final AuditLogger auditLogger;

    public RequestPasswordResetUseCase(
            VerificationTokenRepository tokenRepository,
            AuthenticationRepository authenticationRepository,
            VerificationTokenGenerator tokenGenerator,
            EmailSender emailSender,
            AuditLogger auditLogger
    ) {
        this.tokenRepository = tokenRepository;
        this.authenticationRepository = authenticationRepository;
        this.tokenGenerator = tokenGenerator;
        this.emailSender = emailSender;
        this.auditLogger = auditLogger;
    }

    public void execute(RequestPasswordResetRequest request) {
        this.authenticationRepository.findByEmail(request.email())
                .filter(auth -> !auth.isDeleted())
                .filter(Authentication::isActive)
                .ifPresent(auth -> {
                    String otp = this.tokenGenerator.generateOtp();
                    String otpHash = this.tokenGenerator.hash(otp);

                    this.tokenRepository.invalidatePrevious(auth.getId(), VerificationTokenType.PASSWORD_RESET);

                    VerificationToken token = VerificationToken.create(
                            auth.getId(),
                            otpHash,
                            VerificationTokenType.PASSWORD_RESET
                    );

                    this.tokenRepository.save(token);
                    this.emailSender.sendPasswordOtp(auth.getEmail(), otp, request.lang());
                    this.auditLogger.log(new AuditEvent(
                            AuditEventType.AUTH_PASSWORD_RESET_REQUESTED,
                            auth.getId(),
                            auth.getId(),
                            null,
                            Map.of("lang", String.valueOf(request.lang())),
                            Instant.now()
                    ));
                });
    }
}
