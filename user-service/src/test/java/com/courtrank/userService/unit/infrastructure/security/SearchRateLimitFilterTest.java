package com.courtrank.userService.unit.infrastructure.security;

import com.courtrank.userService.infrastructure.security.SearchRateLimitFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchRateLimitFilterTest {
    @Test
    void searchRoute_shouldRejectRequestsAboveLimit() throws Exception {
        SearchRateLimitFilter filter = new SearchRateLimitFilter(2, 60, fixedClock());
        AtomicInteger passed = new AtomicInteger();
        FilterChain chain = (request, response) -> passed.incrementAndGet();

        MockHttpServletResponse first = new MockHttpServletResponse();
        filter.doFilter(this.searchRequest(), first, chain);

        MockHttpServletResponse second = new MockHttpServletResponse();
        filter.doFilter(this.searchRequest(), second, chain);

        MockHttpServletResponse third = new MockHttpServletResponse();
        filter.doFilter(this.searchRequest(), third, chain);

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(200);
        assertThat(third.getStatus()).isEqualTo(429);
        assertThat(passed.get()).isEqualTo(2);
    }

    @Test
    void nonSearchRoute_shouldBypassLimit() throws Exception {
        SearchRateLimitFilter filter = new SearchRateLimitFilter(1, 60, fixedClock());
        AtomicInteger passed = new AtomicInteger();
        FilterChain chain = (request, response) -> passed.incrementAndGet();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(passed.get()).isEqualTo(1);
    }

    private MockHttpServletRequest searchRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/search");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-06-03T00:00:00Z"), ZoneOffset.UTC);
    }
}
