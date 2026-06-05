package com.example.userService.infrastructure.security;

import com.example.userService.application.ports.security.TokenService;
import com.example.userService.application.ports.security.AuthSessionVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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
    private final AuthSessionVerifier authSessionVerifier;

    public JwtAuthenticationFilter(TokenService tokenService, AuthSessionVerifier authSessionVerifier) {
        this.tokenService = tokenService;
        this.authSessionVerifier = authSessionVerifier;
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
            if (!this.authSessionVerifier.isActive(sessionId)) {
                SecurityContextHolder.clearContext();
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    new AuthUserPrincipal(userId),
                    null,
                    List.of()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
        }
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
