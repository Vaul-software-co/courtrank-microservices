package com.example.authService.application.services;

import com.example.authService.application.dto.AuthResponse;
import com.example.authService.application.dto.HttpContext;
import com.example.authService.application.ports.security.TokenHasher;
import com.example.authService.application.ports.security.TokenService;
import com.example.authService.domain.entity.Authentication;
import com.example.authService.domain.entity.Session;
import com.example.authService.domain.enums.TokenType;
import com.example.authService.domain.repository.SessionRepository;

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
