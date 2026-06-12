package com.courtrank.socialService.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class InternalApiKeyFilter extends OncePerRequestFilter {
    private static final String INTERNAL_API_KEY_HEADER = "x-internal-api-key";
    private static final String INTERNAL_PATH_PREFIX = "/internal/";

    private final String internalApiKey;

    public InternalApiKeyFilter(String internalApiKey) {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw new IllegalStateException("INTERNAL_API_KEY must be configured for internal endpoints");
        }

        this.internalApiKey = internalApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(INTERNAL_API_KEY_HEADER);
        if (apiKey != null && !apiKey.isBlank() && this.matches(apiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"Invalid internal API key\"}");
    }

    private boolean matches(String apiKey) {
        return MessageDigest.isEqual(apiKey.getBytes(StandardCharsets.UTF_8), this.internalApiKey.getBytes(StandardCharsets.UTF_8));
    }
}
