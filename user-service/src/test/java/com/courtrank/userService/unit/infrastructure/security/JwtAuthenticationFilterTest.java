package com.courtrank.userService.unit.infrastructure.security;

import com.courtrank.userService.application.ports.security.AuthSessionVerifier;
import com.courtrank.userService.application.ports.security.TokenService;
import com.courtrank.userService.infrastructure.security.AuthUserPrincipal;
import com.courtrank.userService.infrastructure.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

public class JwtAuthenticationFilterTest {
    private static final String TOKEN = "access-token";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_shouldAuthenticateWhenTokenIsValidAndSessionIsActive() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                new FakeTokenService(userId, sessionId, true),
                session -> true
        );
        MockHttpServletRequest request = this.requestWithBearerToken();

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AuthUserPrincipal principal = assertInstanceOf(AuthUserPrincipal.class, authentication.getPrincipal());
        assertEquals(userId, principal.userId());
        assertEquals("SUPER_ADMIN", principal.role());
        assertEquals("ROLE_SUPER_ADMIN", authentication.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void doFilter_shouldNotAuthenticateWhenSessionIsNotActive() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        AuthSessionVerifier inactiveSessionVerifier = session -> false;
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                new FakeTokenService(userId, sessionId, true),
                inactiveSessionVerifier
        );
        MockHttpServletRequest request = this.requestWithBearerToken();

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private MockHttpServletRequest requestWithBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + TOKEN);
        return request;
    }

    private record FakeTokenService(UUID userId, UUID sessionId, boolean valid) implements TokenService {
        @Override
        public boolean verifyAccess(String token) {
            return valid && TOKEN.equals(token);
        }

        @Override
        public UUID getTokenId(String token) {
            return userId;
        }

        @Override
        public UUID getSessionId(String token) {
            return sessionId;
        }

        @Override
        public String getRole(String token) {
            return "SUPER_ADMIN";
        }
    }
}
