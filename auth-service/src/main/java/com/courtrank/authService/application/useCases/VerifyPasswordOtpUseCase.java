package com.courtrank.authService.application.useCases;

import com.courtrank.authService.application.dto.VerifyPasswordOtpRequest;
import com.courtrank.authService.application.dto.VerifyPasswordOtpResponse;
import com.courtrank.authService.application.ports.audit.AuditEvent;
import com.courtrank.authService.application.ports.audit.AuditEventType;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.ports.security.TokenHasher;
import com.courtrank.authService.application.ports.security.TokenService;
import com.courtrank.authService.application.ports.security.VerificationTokenGenerator;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.entity.VerificationToken;
import com.courtrank.authService.domain.enums.VerificationTokenType;
import com.courtrank.authService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.domain.repository.VerificationTokenRepository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class VerifyPasswordOtpUseCase {
    private final VerificationTokenRepository tokenRepository;
    private final VerificationTokenGenerator tokenGenerator;
    private final AuthenticationRepository authenticationRepository;
    private final TokenService tokenService;
    private final TokenHasher tokenHasher;
    private final AuditLogger auditLogger;

    public VerifyPasswordOtpUseCase(
            VerificationTokenRepository tokenRepository,
            VerificationTokenGenerator tokenGenerator,
            AuthenticationRepository authenticationRepository,
            TokenService tokenService,
            TokenHasher tokenHasher,
            AuditLogger auditLogger
    ) {
        this.tokenRepository = tokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.authenticationRepository = authenticationRepository;
        this.tokenService = tokenService;
        this.tokenHasher = tokenHasher;
        this.auditLogger = auditLogger;
    }

    public VerifyPasswordOtpResponse execute(VerifyPasswordOtpRequest request) {
        Authentication auth = this.authenticationRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        if (auth.isDeleted()) throw new InvalidCredentialsException();
        if (!auth.isActive()) throw new InvalidCredentialsException();

        VerificationToken token = this.tokenRepository.findValid(
                        auth.getId(),
                        VerificationTokenType.PASSWORD_RESET
                )
                .orElseThrow(InvalidCredentialsException::new);

        if (!token.isValid()) throw new InvalidCredentialsException();

        String otpHash = this.tokenGenerator.hash(request.otp());
        if (!token.matches(otpHash)) {
            token.incrementAttempts();
            this.tokenRepository.save(token);
            throw new InvalidCredentialsException();
        }

        token.markAsUsed();
        this.tokenRepository.save(token);

        UUID resetTokenId = UUID.randomUUID();
        VerificationToken resetTokenRecord = VerificationToken.create(
                auth.getId(),
                this.tokenHasher.hash(resetTokenId.toString()),
                VerificationTokenType.PASSWORD_RESET_CONFIRMATION
        );
        this.tokenRepository.save(resetTokenRecord);

        String resetToken = this.tokenService.generatePasswordResetToken(auth.getId(), resetTokenId);
        this.auditLogger.log(new AuditEvent(
                AuditEventType.AUTH_PASSWORD_RESET_OTP_VERIFIED,
                auth.getId(),
                auth.getId(),
                null,
                Map.of(),
                Instant.now()
        ));
        return new VerifyPasswordOtpResponse(resetToken);
    }
}
