package com.example.authService.application.useCases;

import com.example.authService.application.dto.VerifyPasswordOtpRequest;
import com.example.authService.application.dto.VerifyPasswordOtpResponse;
import com.example.authService.application.ports.audit.AuditEvent;
import com.example.authService.application.ports.audit.AuditEventType;
import com.example.authService.application.ports.audit.AuditLogger;
import com.example.authService.application.ports.security.TokenHasher;
import com.example.authService.application.ports.security.TokenService;
import com.example.authService.application.ports.security.VerificationTokenGenerator;
import com.example.authService.domain.entity.Authentication;
import com.example.authService.domain.entity.VerificationToken;
import com.example.authService.domain.enums.VerificationTokenType;
import com.example.authService.domain.exceptions.InvalidCredentialsException;
import com.example.authService.domain.repository.AuthenticationRepository;
import com.example.authService.domain.repository.VerificationTokenRepository;

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
