package com.example.userService.unit.infrastructure.security;

import com.example.userService.infrastructure.security.InternalApiKeyFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InternalApiKeyFilterTest {
    @Test
    void constructor_shouldFailWhenInternalApiKeyIsBlank() {
        assertThrows(IllegalStateException.class, () -> new InternalApiKeyFilter(""));
    }

    @Test
    void doFilter_shouldAllowInternalRequestWhenApiKeyMatches() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("internal-key");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/users/username-available");
        request.addHeader("x-internal-api-key", "internal-key");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest());
    }

    @Test
    void doFilter_shouldRejectInternalRequestWhenApiKeyDoesNotMatch() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("internal-key");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/users/username-available");
        request.addHeader("x-internal-api-key", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(403, response.getStatus());
        assertEquals("{\"error\":\"Invalid internal API key\"}", response.getContentAsString());
    }
}
