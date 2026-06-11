package com.courtrank.authService.infrastructure.security;

import com.courtrank.authService.application.ports.security.ApiClient;
import com.courtrank.authService.application.ports.security.ClientVerifier;
import com.courtrank.authService.domain.exceptions.InvalidCredentialsException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

public class ApiKeyFilter extends OncePerRequestFilter {
    public static final String API_CLIENT_ATTRIBUTE = "apiClient";
    private static final String API_KEY_HEADER = "x-api-key";

    private final ClientVerifier clientVerifier;
    private final Set<Route> protectedPublicRoutes;

    public ApiKeyFilter(ClientVerifier clientVerifier) {
        this.clientVerifier = clientVerifier;
        this.protectedPublicRoutes = Set.of(
                new Route(HttpMethod.POST.name(), "/auth/signup"),
                new Route(HttpMethod.POST.name(), "/auth/signin"),
                new Route(HttpMethod.POST.name(), "/auth/refresh"),
                new Route(HttpMethod.POST.name(), "/auth/verify-email"),
                new Route(HttpMethod.POST.name(), "/auth/verify-email/confirm"),
                new Route(HttpMethod.POST.name(), "/auth/resend-verification-email"),
                new Route(HttpMethod.POST.name(), "/auth/verify-email/resend"),
                new Route(HttpMethod.POST.name(), "/auth/request-password-reset"),
                new Route(HttpMethod.POST.name(), "/auth/password-reset/request"),
                new Route(HttpMethod.POST.name(), "/auth/verify-password-otp"),
                new Route(HttpMethod.POST.name(), "/auth/password-reset/verify"),
                new Route(HttpMethod.POST.name(), "/auth/reset-password"),
                new Route(HttpMethod.PUT.name(), "/auth/password-reset/confirm")
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!this.requiresApiKey(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            ApiClient client = this.clientVerifier.verify(request.getHeader(API_KEY_HEADER));
            request.setAttribute(API_CLIENT_ATTRIBUTE, client);
            filterChain.doFilter(request, response);
        } catch (InvalidCredentialsException exception) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Invalid API key\"}");
        }
    }

    private boolean requiresApiKey(HttpServletRequest request) {
        return this.protectedPublicRoutes.contains(new Route(request.getMethod(), request.getRequestURI()));
    }

    private record Route(String method, String path) {
    }
}
