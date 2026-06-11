package com.courtrank.authService.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRateLimitFilter extends OncePerRequestFilter {
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final Map<Route, LimitRule> rules;
    private final Clock clock;

    public InMemoryRateLimitFilter(Clock clock) {
        this.clock = clock;
        this.rules = Map.ofEntries(
                Map.entry(new Route(HttpMethod.POST.name(), "/auth/signup"), new LimitRule(5, Duration.ofHours(1))),
                Map.entry(new Route(HttpMethod.POST.name(), "/auth/signin"), new LimitRule(5, Duration.ofMinutes(15))),
                Map.entry(new Route(HttpMethod.POST.name(), "/auth/resend-verification-email"), new LimitRule(3, Duration.ofHours(1))),
                Map.entry(new Route(HttpMethod.POST.name(), "/auth/verify-email/resend"), new LimitRule(3, Duration.ofHours(1))),
                Map.entry(new Route(HttpMethod.POST.name(), "/auth/request-password-reset"), new LimitRule(3, Duration.ofHours(1))),
                Map.entry(new Route(HttpMethod.POST.name(), "/auth/password-reset/request"), new LimitRule(3, Duration.ofHours(1))),
                Map.entry(new Route(HttpMethod.POST.name(), "/auth/verify-password-otp"), new LimitRule(5, Duration.ofMinutes(15))),
                Map.entry(new Route(HttpMethod.POST.name(), "/auth/password-reset/verify"), new LimitRule(5, Duration.ofMinutes(15))),
                Map.entry(new Route(HttpMethod.POST.name(), "/auth/reset-password"), new LimitRule(5, Duration.ofMinutes(15))),
                Map.entry(new Route(HttpMethod.PUT.name(), "/auth/password-reset/confirm"), new LimitRule(5, Duration.ofMinutes(15))),
                Map.entry(new Route(HttpMethod.POST.name(), "/auth/refresh"), new LimitRule(60, Duration.ofMinutes(1))),
                Map.entry(new Route(HttpMethod.POST.name(), "/auth/change-password"), new LimitRule(5, Duration.ofMinutes(15)))
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Optional<LimitRule> rule = this.ruleFor(request);
        if (rule.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitDecision decision = this.consume(this.key(request), rule.get());
        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
        response.getWriter().write("{\"error\":\"Too many requests\"}");
    }

    private Optional<LimitRule> ruleFor(HttpServletRequest request) {
        return Optional.ofNullable(this.rules.get(new Route(request.getMethod(), request.getRequestURI())));
    }

    private String key(HttpServletRequest request) {
        return request.getMethod() + ":" + request.getRequestURI() + ":" + this.ip(request);
    }

    private String ip(HttpServletRequest request) {
        String forwardedFor = request.getHeader("x-forwarded-for");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private RateLimitDecision consume(String key, LimitRule rule) {
        long now = this.clock.millis();
        WindowCounter counter = this.counters.computeIfAbsent(key, ignored -> new WindowCounter(now, 0));

        synchronized (counter) {
            long elapsed = now - counter.windowStartedAtMillis;
            if (elapsed >= rule.window().toMillis()) {
                counter.windowStartedAtMillis = now;
                counter.count = 0;
            }

            if (counter.count >= rule.maxRequests()) {
                long retryAfter = Math.max(1, (rule.window().toMillis() - (now - counter.windowStartedAtMillis)) / 1000);
                return new RateLimitDecision(false, retryAfter);
            }

            counter.count++;
            return new RateLimitDecision(true, 0);
        }
    }

    public void clear() {
        this.counters.clear();
    }

    private record Route(String method, String path) {
    }

    private record LimitRule(int maxRequests, Duration window) {
    }

    private record RateLimitDecision(boolean allowed, long retryAfterSeconds) {
    }

    private static class WindowCounter {
        private long windowStartedAtMillis;
        private int count;

        private WindowCounter(long windowStartedAtMillis, int count) {
            this.windowStartedAtMillis = windowStartedAtMillis;
            this.count = count;
        }
    }
}
