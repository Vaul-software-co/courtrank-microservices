package com.courtrank.authService.application.services;

import com.courtrank.authService.application.dto.AuthResponse;
import com.courtrank.authService.application.dto.HttpContext;
import com.courtrank.authService.application.ports.security.TokenHasher;
import com.courtrank.authService.application.ports.security.TokenService;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.entity.Session;
import com.courtrank.authService.domain.enums.TokenType;
import com.courtrank.authService.domain.repository.SessionRepository;

import java.util.Optional;

public class SessionIssuer {
    private final SessionRepository sessionRepository;
    private final TokenService tokenService;
    private final TokenHasher tokenHasher;

    public SessionIssuer(
            SessionRepository sessionRepository,
            TokenService tokenService,
            TokenHasher tokenHasher
    ) {
        this.sessionRepository = sessionRepository;
        this.tokenService = tokenService;
        this.tokenHasher = tokenHasher;
    }

    public AuthResponse issue(Authentication auth, HttpContext http) {
        String refreshToken = this.tokenService.generateToken(auth.getId(), TokenType.REFRESH);
        String hashedRefreshToken = this.tokenHasher.hash(refreshToken);
        Session session = Session.create(auth.getId(), hashedRefreshToken, http.client(), http.ip(), http.userAgent());

        this.sessionRepository.save(session);

        String accessToken = this.tokenService.generateAccessToken(auth.getId(), session.getId(), auth.getRole());
        return new AuthResponse(accessToken, refreshToken, Optional.empty());
    }
}
