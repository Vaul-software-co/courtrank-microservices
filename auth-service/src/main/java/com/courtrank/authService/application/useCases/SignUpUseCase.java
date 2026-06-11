package com.courtrank.authService.application.useCases;

import com.courtrank.authService.application.dto.AuthResponse;
import com.courtrank.authService.application.dto.HttpContext;
import com.courtrank.authService.application.dto.SignUpRequest;
import com.courtrank.authService.application.dto.SignUpResponse;
import com.courtrank.authService.application.events.UserRegisteredEvent;
import com.courtrank.authService.application.events.UserRestoredEvent;
import com.courtrank.authService.application.ports.AuthEventPublisher;
import com.courtrank.authService.application.ports.audit.AuditEvent;
import com.courtrank.authService.application.ports.audit.AuditEventType;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.ports.user.UsernameAvailabilityVerifier;
import com.courtrank.authService.application.services.SessionIssuer;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.enums.UserRole;
import com.courtrank.authService.domain.exceptions.ConflictException;
import com.courtrank.authService.domain.exceptions.MissedTermsAndConditionsException;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.application.ports.security.PasswordHasher;
import com.courtrank.authService.domain.service.PasswordPolicy;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public class SignUpUseCase {
    private final AuthenticationRepository authRepository;
    private final PasswordHasher passwordHasher;
    private final AuthEventPublisher eventPublisher;
    private final UsernameAvailabilityVerifier usernameAvailabilityVerifier;
    private final PasswordPolicy passwordPolicy;
    private final AuditLogger auditLogger;
    private final SessionIssuer sessionIssuer;

    public SignUpUseCase(
            AuthenticationRepository authRepository,
            PasswordHasher passwordHasher,
            AuthEventPublisher eventPublisher,
            UsernameAvailabilityVerifier usernameAvailabilityVerifier,
            PasswordPolicy passwordPolicy,
            AuditLogger auditLogger,
            SessionIssuer sessionIssuer
    ){
        this.authRepository = authRepository;
        this.passwordHasher = passwordHasher;
        this.eventPublisher = eventPublisher;
        this.usernameAvailabilityVerifier = usernameAvailabilityVerifier;
        this.passwordPolicy = passwordPolicy;
        this.auditLogger = auditLogger;
        this.sessionIssuer = sessionIssuer;
    }

    public SignUpResponse execute(SignUpRequest request, HttpContext http){
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

        if (request.username() != null && !request.username().isBlank()) {
            this.usernameAvailabilityVerifier.assertAvailable(request.username(), auth.getId());
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
                            auth.isEmailVerified(),
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
                            auth.isEmailVerified(),
                            request.termsVersion(),
                            request.isCommercial(),
                            Instant.now()
                    )
            );
        }

        Optional<AuthResponse> authResponse = Optional.empty();

        if (auth.getRole() == UserRole.MEMBER) {
            authResponse = Optional.of(this.sessionIssuer.issue(auth, http));
        }

        return new SignUpResponse(auth, authResponse);
    }
}
