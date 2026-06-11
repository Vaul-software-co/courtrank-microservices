package com.courtrank.authService.unit.infrastructure.user;

import com.courtrank.authService.domain.exceptions.ConflictException;
import com.courtrank.authService.domain.exceptions.UserServiceUnavailableException;
import com.courtrank.authService.infrastructure.user.HttpUsernameAvailabilityVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpStatus.CONFLICT;

public class HttpUsernameAvailabilityVerifierTest {
    private static final String BASE_URL = "http://localhost:8082/internal";
    private static final String INTERNAL_API_KEY = "internal-key";

    @Test
    void assertAvailable_shouldPassWhenUserServiceReturnsAvailable() {
        UUID userId = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpUsernameAvailabilityVerifier verifier = new HttpUsernameAvailabilityVerifier(builder.build(), INTERNAL_API_KEY);

        server.expect(once(), requestTo(BASE_URL + "/users/username-available?username=sebas&userId=" + userId))
                .andExpect(method(GET))
                .andExpect(header("x-internal-api-key", INTERNAL_API_KEY))
                .andRespond(withSuccess("{\"available\":true}", MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> verifier.assertAvailable("sebas", userId));
        server.verify();
    }

    @Test
    void assertAvailable_shouldThrowConflictWhenUserServiceReturnsUnavailable() {
        UUID userId = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpUsernameAvailabilityVerifier verifier = new HttpUsernameAvailabilityVerifier(builder.build(), INTERNAL_API_KEY);

        server.expect(once(), requestTo(BASE_URL + "/users/username-available?username=sebas&userId=" + userId))
                .andExpect(method(GET))
                .andExpect(header("x-internal-api-key", INTERNAL_API_KEY))
                .andRespond(withSuccess("{\"available\":false}", MediaType.APPLICATION_JSON));

        assertThrows(ConflictException.class, () -> verifier.assertAvailable("sebas", userId));
        server.verify();
    }

    @Test
    void assertAvailable_shouldThrowConflictWhenUserServiceReturnsConflict() {
        UUID userId = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpUsernameAvailabilityVerifier verifier = new HttpUsernameAvailabilityVerifier(builder.build(), INTERNAL_API_KEY);

        server.expect(once(), requestTo(BASE_URL + "/users/username-available?username=sebas&userId=" + userId))
                .andExpect(method(GET))
                .andExpect(header("x-internal-api-key", INTERNAL_API_KEY))
                .andRespond(withStatus(CONFLICT));

        assertThrows(ConflictException.class, () -> verifier.assertAvailable("sebas", userId));
        server.verify();
    }

    @Test
    void assertAvailable_shouldThrowUnavailableWhenUserServiceFails() {
        UUID userId = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpUsernameAvailabilityVerifier verifier = new HttpUsernameAvailabilityVerifier(builder.build(), INTERNAL_API_KEY);

        server.expect(once(), requestTo(BASE_URL + "/users/username-available?username=sebas&userId=" + userId))
                .andExpect(method(GET))
                .andExpect(header("x-internal-api-key", INTERNAL_API_KEY))
                .andRespond(withServerError());

        assertThrows(UserServiceUnavailableException.class, () -> verifier.assertAvailable("sebas", userId));
        server.verify();
    }
}
