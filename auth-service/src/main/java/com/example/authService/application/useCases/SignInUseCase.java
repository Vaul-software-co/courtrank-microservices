package com.example.authService.application.useCases;

import com.example.authService.application.dto.HttpContext;
import com.example.authService.application.dto.AuthResponse;
import com.example.authService.application.dto.SignInRequest;
import com.example.authService.application.ports.authorization.WorkerAccess;
import com.example.authService.application.ports.authorization.WorkerAccessVerifier;
import com.example.authService.application.ports.audit.AuditEvent;
import com.example.authService.application.ports.audit.AuditEventType;
import com.example.authService.application.ports.audit.AuditLogger;
import com.example.authService.domain.entity.Authentication;
import com.example.authService.domain.entity.Session;
import com.example.authService.domain.enums.TokenType;
import com.example.authService.domain.enums.UserRole;
import com.example.authService.domain.exceptions.DisabledAccountException;
import com.example.authService.domain.exceptions.EmailNotVerifiedException;
import com.example.authService.domain.exceptions.ForbiddenException;
import com.example.authService.domain.exceptions.InvalidCredentialsException;
import com.example.authService.domain.repository.AuthenticationRepository;
import com.example.authService.domain.repository.SessionRepository;
import com.example.authService.application.ports.security.PasswordHasher;
import com.example.authService.application.ports.security.TokenHasher;
import com.example.authService.application.ports.security.TokenService;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SignInUseCase {

    private final AuthenticationRepository authenticationRepository;
    private final PasswordHasher passwordHasher;
    private final SessionRepository sessionRepository;
    private final TokenService tokenService;
    private final TokenHasher tokenHasher;
    private final WorkerAccessVerifier workerAccess;
    private final AuditLogger auditLogger;

    public SignInUseCase(
            AuthenticationRepository authenticationRepository,
            PasswordHasher passwordHasher,
            SessionRepository sessionRepository,
            TokenService tokenService,
            TokenHasher tokenHasher,
            WorkerAccessVerifier workerAccess,
            AuditLogger auditLogger
    ) {
        this.authenticationRepository = authenticationRepository;
        this.passwordHasher = passwordHasher;
        this.sessionRepository = sessionRepository;
        this.tokenService = tokenService;
        this.tokenHasher = tokenHasher;
        this.workerAccess = workerAccess;
        this.auditLogger = auditLogger;
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

        if (!auth.isEmailVerified()) {
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

        String refreshToken = this.tokenService.generateToken(auth.getId(), TokenType.REFRESH);

        String hashedRefreshToken = this.tokenHasher.hash(refreshToken);

        Session session = Session.create(auth.getId(), hashedRefreshToken, http.client(), http.ip(), http.userAgent());

        this.sessionRepository.save(session);
        String accessToken = this.tokenService.generateAccessToken(auth.getId(), session.getId(), auth.getRole());
        this.auditLogger.log(new AuditEvent(
                AuditEventType.AUTH_SIGN_IN_SUCCESS,
                auth.getId(),
                auth.getId(),
                http.traceId(),
                Map.of(
                        "client", http.client(),
                        "ip", http.ip(),
                        "sessionId", session.getId().toString()
                ),
                Instant.now()
        ));

        return new AuthResponse(accessToken, refreshToken, Optional.ofNullable(clubId));
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
