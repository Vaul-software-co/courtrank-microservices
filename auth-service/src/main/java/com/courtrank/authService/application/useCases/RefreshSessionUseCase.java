package com.courtrank.authService.application.useCases;

import com.courtrank.authService.application.dto.AuthResponse;
import com.courtrank.authService.application.dto.RefreshSessionRequest;
import com.courtrank.authService.application.ports.audit.AuditEvent;
import com.courtrank.authService.application.ports.audit.AuditEventType;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.ports.security.TokenHasher;
import com.courtrank.authService.application.ports.security.TokenService;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.entity.Session;
import com.courtrank.authService.domain.enums.TokenType;
import com.courtrank.authService.domain.exceptions.DisabledAccountException;
import com.courtrank.authService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.domain.repository.SessionRepository;
import com.courtrank.authService.domain.repository.results.SessionRotationResult;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class RefreshSessionUseCase {
    private final TokenService tokenService;
    private final TokenHasher tokenHasher;
    private final SessionRepository sessionRepository;
    private final AuthenticationRepository authenticationRepository;
    private final AuditLogger auditLogger;

    public RefreshSessionUseCase(
            TokenService tokenService,
            TokenHasher tokenHasher,
            SessionRepository sessionRepository,
            AuthenticationRepository authenticationRepository,
            AuditLogger auditLogger
    ) {
        this.tokenService = tokenService;
        this.tokenHasher = tokenHasher;
        this.sessionRepository = sessionRepository;
        this.authenticationRepository = authenticationRepository;
        this.auditLogger = auditLogger;
    }

    public AuthResponse execute(RefreshSessionRequest request) {
        if (!this.tokenService.verifyRefresh(request.refreshToken())) {
            throw new InvalidCredentialsException();
        }

        UUID userId = this.tokenService.getTokenId(request.refreshToken());
        Authentication auth = this.authenticationRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);

        if (auth.isDeleted()) throw new InvalidCredentialsException();
        if (!auth.isActive()) throw new DisabledAccountException();

        String oldRefreshTokenHash = this.tokenHasher.hash(request.refreshToken());
        String newRefreshToken = this.tokenService.generateToken(auth.getId(), TokenType.REFRESH);
        String newRefreshTokenHash = this.tokenHasher.hash(newRefreshToken);

        Session newSession = Session.create(
                auth.getId(),
                newRefreshTokenHash,
                request.http().client(),
                request.http().ip(),
                request.http().userAgent()
        );

        SessionRotationResult rotation = this.sessionRepository.rotateSession(oldRefreshTokenHash, newSession);

        if (rotation.alreadyRevoked()) {
            if (!rotation.recentRotation()) {
                this.sessionRepository.revokeAllByUserId(rotation.oldSession().getUserId());
                this.auditLogger.log(new AuditEvent(
                        AuditEventType.AUTH_REFRESH_REUSE_DETECTED,
                        rotation.oldSession().getUserId(),
                        rotation.oldSession().getUserId(),
                        request.http().traceId(),
                        Map.of(
                                "client", request.http().client(),
                                "recentRotation", false,
                                "sessionsRevoked", true
                        ),
                        Instant.now()
                ));
            }
            throw new InvalidCredentialsException();
        }

        if (!rotation.oldSession().isActive()) {
            throw new InvalidCredentialsException();
        }

        String accessToken = this.tokenService.generateAccessToken(auth.getId(), newSession.getId(), auth.getRole());

        this.auditLogger.log(new AuditEvent(
                AuditEventType.AUTH_SESSION_REFRESHED,
                auth.getId(),
                auth.getId(),
                request.http().traceId(),
                Map.of(
                        "client", request.http().client(),
                        "oldSessionId", rotation.oldSession().getId().toString(),
                        "newSessionId", newSession.getId().toString()
                ),
                Instant.now()
        ));

        return new AuthResponse(accessToken, newRefreshToken, Optional.empty());
    }
}
