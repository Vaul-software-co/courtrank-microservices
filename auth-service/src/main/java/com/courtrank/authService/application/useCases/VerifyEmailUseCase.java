package com.courtrank.authService.application.useCases;

import com.courtrank.authService.application.events.UserEmailVerifiedEvent;
import com.courtrank.authService.application.ports.AuthEventPublisher;
import com.courtrank.authService.application.dto.VerifyEmailRequest;
import com.courtrank.authService.application.ports.audit.AuditEvent;
import com.courtrank.authService.application.ports.audit.AuditEventType;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.ports.security.PasswordHasher;
import com.courtrank.authService.application.ports.security.VerificationTokenGenerator;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.entity.VerificationToken;
import com.courtrank.authService.domain.enums.VerificationTokenType;
import com.courtrank.authService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.domain.repository.VerificationTokenRepository;

import java.time.Instant;
import java.util.Map;

public class VerifyEmailUseCase {
    private final VerificationTokenRepository tokenRepository;
    private final VerificationTokenGenerator tokenGenerator;
    private final AuthenticationRepository authenticationRepository;
    private final PasswordHasher passwordHasher;
    private final AuditLogger auditLogger;
    private final AuthEventPublisher eventPublisher;

    public VerifyEmailUseCase(
            VerificationTokenRepository tokenRepository,
            VerificationTokenGenerator tokenGenerator,
            AuthenticationRepository authenticationRepository,
            PasswordHasher passwordHasher,
            AuditLogger auditLogger,
            AuthEventPublisher eventPublisher
    ) {
        this.tokenRepository = tokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.authenticationRepository = authenticationRepository;
        this.passwordHasher = passwordHasher;
        this.auditLogger = auditLogger;
        this.eventPublisher = eventPublisher;
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
        this.eventPublisher.publishUserEmailVerified(new UserEmailVerifiedEvent(auth.getId(), auth.getEmail(), Instant.now()));
    }
}
