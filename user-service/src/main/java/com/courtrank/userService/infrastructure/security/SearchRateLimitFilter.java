package com.courtrank.userService.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SearchRateLimitFilter extends OncePerRequestFilter {
    private static final String SEARCH_PATH = "/users/search";
    private static final String X_FORWARDED_FOR_HEADER = "x-forwarded-for";

    private final int maxRequests;
    private final long windowMillis;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public SearchRateLimitFilter(int maxRequests, long windowSeconds) {
        this(maxRequests, windowSeconds, Clock.systemUTC());
    }

    public SearchRateLimitFilter(int maxRequests, long windowSeconds, Clock clock) {
        this.maxRequests = Math.max(1, maxRequests);
        this.windowMillis = Math.max(1, windowSeconds) * 1000;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!SEARCH_PATH.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        Window window = this.windows.compute(this.key(request), (key, current) -> this.nextWindow(current));

        if (window.count() <= this.maxRequests) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"Too many search requests\"}");
    }

    private Window nextWindow(Window current) {
        long now = this.clock.millis();
        if (current == null || now >= current.resetAtMillis()) {
            return new Window(now + this.windowMillis, 1);
        }

        return new Window(current.resetAtMillis(), current.count() + 1);
    }

    private String key(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR_HEADER);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private record Window(
            long resetAtMillis,
            int count
    ) {
    }
}
