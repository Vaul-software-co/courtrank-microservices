package com.courtrank.userService.unit.infrastructure.security;

import com.courtrank.userService.infrastructure.security.HttpAuthSessionVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpAuthSessionVerifierTest {
    private static final String BASE_URL = "http://localhost:8081/internal";
    private static final String INTERNAL_API_KEY = "internal-key";

    @Test
    void constructor_shouldRejectBlankInternalApiKey() {
        RestClient restClient = RestClient.builder().baseUrl(BASE_URL).build();

        assertThatThrownBy(() -> new HttpAuthSessionVerifier(restClient, " "))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void isActive_shouldReturnTrueWhenAuthServiceReturnsActiveSession() {
        UUID sessionId = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpAuthSessionVerifier verifier = new HttpAuthSessionVerifier(builder.build(), INTERNAL_API_KEY);

        server.expect(once(), requestTo(BASE_URL + "/auth/sessions/" + sessionId + "/active"))
                .andExpect(method(GET))
                .andExpect(header("x-internal-api-key", INTERNAL_API_KEY))
                .andRespond(withSuccess("{\"active\":true}", MediaType.APPLICATION_JSON));

        assertThat(verifier.isActive(sessionId)).isTrue();
        server.verify();
    }

    @Test
    void isActive_shouldReturnFalseWhenAuthServiceReturnsInactiveSession() {
        UUID sessionId = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpAuthSessionVerifier verifier = new HttpAuthSessionVerifier(builder.build(), INTERNAL_API_KEY);

        server.expect(once(), requestTo(BASE_URL + "/auth/sessions/" + sessionId + "/active"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"active\":false}", MediaType.APPLICATION_JSON));

        assertThat(verifier.isActive(sessionId)).isFalse();
        server.verify();
    }

    @Test
    void isActive_shouldReturnFalseWhenAuthServiceFails() {
        UUID sessionId = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpAuthSessionVerifier verifier = new HttpAuthSessionVerifier(builder.build(), INTERNAL_API_KEY);

        server.expect(once(), requestTo(BASE_URL + "/auth/sessions/" + sessionId + "/active"))
                .andExpect(method(GET))
                .andRespond(withServerError());

        assertThat(verifier.isActive(sessionId)).isFalse();
        server.verify();
    }
}
