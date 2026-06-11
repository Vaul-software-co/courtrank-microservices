package com.courtrank.authService.infrastructure.security;

import com.courtrank.authService.application.ports.security.TokenService;
import com.courtrank.authService.domain.entity.Session;
import com.courtrank.authService.domain.repository.SessionRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_COOKIE = "token";

    private final TokenService tokenService;
    private final SessionRepository sessionRepository;

    public JwtAuthenticationFilter(TokenService tokenService, SessionRepository sessionRepository) {
        this.tokenService = tokenService;
        this.sessionRepository = sessionRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = this.extractBearerToken(request);

        if (token != null && this.tokenService.verifyAccess(token)) {
            this.authenticateFromToken(token);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateFromToken(String token) {
        try {
            UUID userId = this.tokenService.getTokenId(token);
            UUID sessionId = this.tokenService.getSessionId(token);

            this.sessionRepository.findById(sessionId)
                    .filter(session -> this.belongsToUser(session, userId))
                    .filter(Session::isActive)
                    .ifPresent(session -> this.authenticate(userId));
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean belongsToUser(Session session, UUID userId) {
        return session.getUserId().equals(userId);
    }

    private void authenticate(UUID userId) {
        AuthUserPrincipal principal = new AuthUserPrincipal(userId);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String cookieToken = this.extractCookieToken(request);
        if (cookieToken != null) {
            return cookieToken;
        }

        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    private String extractCookieToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
