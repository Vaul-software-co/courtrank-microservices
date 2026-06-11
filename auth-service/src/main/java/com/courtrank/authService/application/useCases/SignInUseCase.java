package com.courtrank.authService.application.useCases;

import com.courtrank.authService.application.dto.HttpContext;
import com.courtrank.authService.application.dto.AuthResponse;
import com.courtrank.authService.application.dto.SignInRequest;
import com.courtrank.authService.application.ports.authorization.WorkerAccess;
import com.courtrank.authService.application.ports.authorization.WorkerAccessVerifier;
import com.courtrank.authService.application.ports.audit.AuditEvent;
import com.courtrank.authService.application.ports.audit.AuditEventType;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.services.SessionIssuer;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.enums.UserRole;
import com.courtrank.authService.domain.exceptions.DisabledAccountException;
import com.courtrank.authService.domain.exceptions.EmailNotVerifiedException;
import com.courtrank.authService.domain.exceptions.ForbiddenException;
import com.courtrank.authService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.application.ports.security.PasswordHasher;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SignInUseCase {

    private final AuthenticationRepository authenticationRepository;
    private final PasswordHasher passwordHasher;
    private final WorkerAccessVerifier workerAccess;
    private final AuditLogger auditLogger;
    private final SessionIssuer sessionIssuer;

    public SignInUseCase(
            AuthenticationRepository authenticationRepository,
            PasswordHasher passwordHasher,
            WorkerAccessVerifier workerAccess,
            AuditLogger auditLogger,
            SessionIssuer sessionIssuer
    ) {
        this.authenticationRepository = authenticationRepository;
        this.passwordHasher = passwordHasher;
        this.workerAccess = workerAccess;
        this.auditLogger = auditLogger;
        this.sessionIssuer = sessionIssuer;
    }

    public AuthResponse execute(SignInRequest request, HttpContext http) {
        Authentication auth = this.authenticationRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        boolean passwordCheck = this.passwordHasher.checkPassword(request.password(), auth.getPasswordHash());

        if (auth.isDeleted()) {
            this.auditSignInFailure(AuditEventType.AUTH_SIGN_IN_FAILED, auth, http, "DELETED_USER");
            throw new InvalidCredentialsException();
        }

        if (!auth.isActive()) {
            this.auditSignInFailure(AuditEventType.AUTH_SIGN_IN_BLOCKED, auth, http, "DISABLED_ACCOUNT");
            throw new DisabledAccountException();
        }

        if (!auth.isEmailVerified() && !auth.getRole().equals(UserRole.MEMBER)) {
            this.auditSignInFailure(AuditEventType.AUTH_SIGN_IN_UNVERIFIED_EMAIL, auth, http, "EMAIL_NOT_VERIFIED");
            throw new EmailNotVerifiedException();
        }

        if (!passwordCheck) {
            this.auditSignInFailure(AuditEventType.AUTH_SIGN_IN_FAILED, auth, http, "INVALID_PASSWORD");
            throw new InvalidCredentialsException();
        }

        UUID clubId = null;
        if (auth.getRole().equals(UserRole.MEMBER) && !http.type().equals(UserRole.MEMBER)) {
            WorkerAccess access = workerAccess.verify(auth.getId());
            if (!access.hasAccess()) {
                this.auditSignInFailure(AuditEventType.AUTH_SIGN_IN_FORBIDDEN, auth, http, "WORKER_ACCESS_DENIED");
                throw new ForbiddenException();
            }

            clubId = access.defaultClubId();
        }

        AuthResponse response = this.sessionIssuer.issue(auth, http);
        this.auditLogger.log(new AuditEvent(
                AuditEventType.AUTH_SIGN_IN_SUCCESS,
                auth.getId(),
                auth.getId(),
                http.traceId(),
                Map.of(
                        "client", http.client(),
                        "ip", http.ip()
                ),
                Instant.now()
        ));

        return new AuthResponse(response.accessToken(), response.refreshToken(), Optional.ofNullable(clubId));
    }

    private void auditSignInFailure(AuditEventType eventType, Authentication auth, HttpContext http, String reason) {
        this.auditLogger.log(new AuditEvent(
                eventType,
                auth.getId(),
                auth.getId(),
                http.traceId(),
                Map.of(
                        "client", http.client(),
                        "ip", http.ip(),
                        "reason", reason
                ),
                Instant.now()
        ));
    }
}
