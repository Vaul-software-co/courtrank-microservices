package com.courtrank.authService.application.useCases;

import com.courtrank.authService.application.dto.ResetPasswordRequest;
import com.courtrank.authService.application.ports.audit.AuditEvent;
import com.courtrank.authService.application.ports.audit.AuditEventType;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.ports.security.PasswordHasher;
import com.courtrank.authService.application.ports.security.TokenHasher;
import com.courtrank.authService.application.ports.security.TokenService;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.entity.VerificationToken;
import com.courtrank.authService.domain.enums.VerificationTokenType;
import com.courtrank.authService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.domain.repository.VerificationTokenRepository;
import com.courtrank.authService.domain.service.PasswordPolicy;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class ResetPasswordUseCase {
    private final AuthenticationRepository authenticationRepository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;
    private final TokenHasher tokenHasher;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordPolicy passwordPolicy;
    private final AuditLogger auditLogger;

    public ResetPasswordUseCase(
            AuthenticationRepository authenticationRepository,
            PasswordHasher passwordHasher,
            TokenService tokenService,
            TokenHasher tokenHasher,
            VerificationTokenRepository verificationTokenRepository,
            PasswordPolicy passwordPolicy,
            AuditLogger auditLogger
    ) {
        this.authenticationRepository = authenticationRepository;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
        this.tokenHasher = tokenHasher;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordPolicy = passwordPolicy;
        this.auditLogger = auditLogger;
    }

    public void execute(ResetPasswordRequest request) {
        if (!this.tokenService.verifyPasswordReset(request.resetToken())) {
            throw new InvalidCredentialsException();
        }

        UUID userId = this.tokenService.getTokenId(request.resetToken());
        Authentication auth = this.authenticationRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);

        if (auth.isDeleted()) throw new InvalidCredentialsException();
        if (!auth.isActive()) throw new InvalidCredentialsException();

        UUID resetTokenId = this.tokenService.getTokenJti(request.resetToken());
        VerificationToken resetTokenRecord = this.verificationTokenRepository.findValid(
                        auth.getId(),
                        this.tokenHasher.hash(resetTokenId.toString()),
                        VerificationTokenType.PASSWORD_RESET_CONFIRMATION
                )
                .orElseThrow(InvalidCredentialsException::new);

        this.passwordPolicy.validate(request.newPassword());
        String passwordHash = this.passwordHasher.hashPassword(request.newPassword());
        auth.changePassword(passwordHash);
        resetTokenRecord.markAsUsed();

        this.verificationTokenRepository.save(resetTokenRecord);
        this.authenticationRepository.save(auth);
        this.auditLogger.log(new AuditEvent(
                AuditEventType.AUTH_PASSWORD_RESET_COMPLETED,
                auth.getId(),
                auth.getId(),
                null,
                Map.of(),
                Instant.now()
        ));
    }
}
