package com.example.authService.application.useCases;

import com.example.authService.application.dto.HttpContext;
import com.example.authService.application.dto.SignUpRequest;
import com.example.authService.application.events.UserRegisteredEvent;
import com.example.authService.application.events.UserRestoredEvent;
import com.example.authService.application.ports.AuthEventPublisher;
import com.example.authService.application.ports.audit.AuditEvent;
import com.example.authService.application.ports.audit.AuditEventType;
import com.example.authService.application.ports.audit.AuditLogger;
import com.example.authService.domain.entity.Authentication;
import com.example.authService.domain.exceptions.ConflictException;
import com.example.authService.domain.exceptions.MissedTermsAndConditionsException;
import com.example.authService.domain.repository.AuthenticationRepository;
import com.example.authService.application.ports.security.PasswordHasher;
import com.example.authService.domain.service.PasswordPolicy;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public class SignUpUseCase {
    private final AuthenticationRepository authRepository;
    private final PasswordHasher passwordHasher;
    private final AuthEventPublisher eventPublisher;
    private final PasswordPolicy passwordPolicy;
    private final AuditLogger auditLogger;

    public SignUpUseCase(
            AuthenticationRepository authRepository,
            PasswordHasher passwordHasher,
            AuthEventPublisher eventPublisher,
            PasswordPolicy passwordPolicy,
            AuditLogger auditLogger
    ){
        this.authRepository = authRepository;
        this.passwordHasher = passwordHasher;
        this.eventPublisher = eventPublisher;
        this.passwordPolicy = passwordPolicy;
        this.auditLogger = auditLogger;
    }

    public Authentication execute(SignUpRequest request, HttpContext http){
        if (!request.isTerms()) {
            throw new MissedTermsAndConditionsException();
        }

        this.passwordPolicy.validate(request.password());
        String passwordHash = this.passwordHasher.hashPassword(request.password());
        Optional<Authentication> existingAuth =
                this.authRepository.findByEmailIncludingDeleted(request.email());

        Authentication auth;
        boolean restored;
        AuditEventType eventType;

        if (existingAuth.isPresent()) {
            auth = existingAuth.orElseThrow();

            if (!auth.isDeleted()) {
                this.auditLogger.log(new AuditEvent(
                        AuditEventType.AUTH_SIGN_UP_CONFLICT,
                        null,
                        existingAuth.get().getId(),
                        http.traceId(),
                        Map.of(
                                "email", request.email(),
                                "client", http.client()
                        ),
                        Instant.now()
                ));
                throw new ConflictException("Email already registered");
            }

            auth.restoreUser(passwordHash);
            eventType = AuditEventType.AUTH_SIGN_UP_RESTORED_USER;
            restored = true;
        } else {
            auth = Authentication.create(request.email(), passwordHash, http.type());
            eventType = AuditEventType.AUTH_SIGN_UP_SUCCESS;
            restored = false;
        }

        if (request.isCommercial()) {
            auth.acceptData();
        }

        auth.acceptTerms(request.termsVersion());

        this.authRepository.save(auth);
        this.auditLogger.log(
                new AuditEvent(
                        eventType,
                        auth.getId(),
                        auth.getId(),
                        http.traceId(),
                        Map.of(
                                "email", auth.getEmail(),
                                "client", http.client(),
                                "acceptedCommercialData", request.isCommercial()
                        ),
                        Instant.now()
                )
        );

        if (restored) {
            this.eventPublisher.publishUserRestored(
                    new UserRestoredEvent(
                            auth.getId(),
                            auth.getEmail(),
                            request.name(),
                            request.username(),
                            auth.getRole(),
                            request.termsVersion(),
                            request.isCommercial(),
                            Instant.now()
                    )
            );
        } else {
            this.eventPublisher.publishUserRegistered(
                    new UserRegisteredEvent(
                            auth.getId(),
                            auth.getEmail(),
                            request.name(),
                            request.username(),
                            auth.getRole(),
                            request.termsVersion(),
                            request.isCommercial(),
                            Instant.now()
                    )
            );
        }

        return auth;
    }
}
