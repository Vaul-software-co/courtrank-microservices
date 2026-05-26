package com.example.authService.application.useCases;

import com.example.authService.application.dto.VerifyEmailRequest;
import com.example.authService.application.ports.audit.AuditEvent;
import com.example.authService.application.ports.audit.AuditEventType;
import com.example.authService.application.ports.audit.AuditLogger;
import com.example.authService.application.ports.security.PasswordHasher;
import com.example.authService.application.ports.security.VerificationTokenGenerator;
import com.example.authService.domain.entity.Authentication;
import com.example.authService.domain.entity.VerificationToken;
import com.example.authService.domain.enums.VerificationTokenType;
import com.example.authService.domain.exceptions.InvalidCredentialsException;
import com.example.authService.domain.repository.AuthenticationRepository;
import com.example.authService.domain.repository.VerificationTokenRepository;

import java.time.Instant;
import java.util.Map;

public class VerifyEmailUseCase {
    private final VerificationTokenRepository tokenRepository;
    private final VerificationTokenGenerator tokenGenerator;
    private final AuthenticationRepository authenticationRepository;
    private final PasswordHasher passwordHasher;
    private final AuditLogger auditLogger;

    public VerifyEmailUseCase(VerificationTokenRepository tokenRepository, VerificationTokenGenerator tokenGenerator, AuthenticationRepository authenticationRepository, PasswordHasher passwordHasher, AuditLogger auditLogger) {
        this.tokenRepository = tokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.authenticationRepository = authenticationRepository;
        this.passwordHasher = passwordHasher;
        this.auditLogger = auditLogger;
    }


    public void execute(VerifyEmailRequest request){
        String tokenHash = this.tokenGenerator.hash(request.token());
        VerificationToken token = this.tokenRepository.findValid(
                request.userId(),
                tokenHash,
                VerificationTokenType.EMAIL_VERIFICATION
        )
                                        .orElseThrow(InvalidCredentialsException::new);
        if(!token.isValid()) throw new InvalidCredentialsException();

        Authentication auth = this.authenticationRepository.findById(request.userId())
                                        .orElseThrow(InvalidCredentialsException::new);
        if (auth.isDeleted()) throw new InvalidCredentialsException();
        if (!auth.isActive()) throw new InvalidCredentialsException();

        token.incrementAttempts();

        if(!this.passwordHasher.checkPassword(request.password(),auth.getPasswordHash())){
            this.tokenRepository.save(token);
            throw new InvalidCredentialsException();
        }

        auth.verifyEmail();
        token.markAsUsed();
        this.authenticationRepository.save(auth);
        this.tokenRepository.save(token);
        this.auditLogger.log(new AuditEvent(
                AuditEventType.AUTH_EMAIL_VERIFIED,
                auth.getId(),
                auth.getId(),
                null,
                Map.of(),
                Instant.now()
        ));

    }
}
