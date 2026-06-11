package com.courtrank.authService.unit.infrastructure.authorization;

import com.courtrank.authService.application.ports.authorization.WorkerAccess;
import com.courtrank.authService.infrastructure.authorization.HttpWorkerAccessVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

public class HttpWorkerAccessVerifierTest {
    private static final String BASE_URL = "http://localhost:4000/internal";
    private static final String INTERNAL_API_KEY = "internal-key";

    @Test
    void verify_shouldReturnWorkerAccessWhenServiceAllowsUser() {
        UUID userId = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpWorkerAccessVerifier verifier = new HttpWorkerAccessVerifier(builder.build(), INTERNAL_API_KEY);

        server.expect(once(), requestTo(BASE_URL + "/users/" + userId + "/worker-access"))
                .andExpect(method(GET))
                .andExpect(header("x-internal-api-key", INTERNAL_API_KEY))
                .andRespond(withSuccess("""
                        {
                          "hasAccess": true,
                          "defaultClubId": "%s"
                        }
                        """.formatted(clubId), MediaType.APPLICATION_JSON));

        WorkerAccess access = verifier.verify(userId);

        assertTrue(access.hasAccess());
        assertEquals(clubId, access.defaultClubId());
        server.verify();
    }

    @Test
    void verify_shouldReturnDeniedWhenServiceDeniesUser() {
        UUID userId = UUID.randomUUID();

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpWorkerAccessVerifier verifier = new HttpWorkerAccessVerifier(builder.build(), INTERNAL_API_KEY);

        server.expect(once(), requestTo(BASE_URL + "/users/" + userId + "/worker-access"))
                .andExpect(method(GET))
                .andExpect(header("x-internal-api-key", INTERNAL_API_KEY))
                .andRespond(withSuccess("""
                        {
                          "hasAccess": false,
                          "defaultClubId": null
                        }
                        """, MediaType.APPLICATION_JSON));

        WorkerAccess access = verifier.verify(userId);

        assertFalse(access.hasAccess());
        assertNull(access.defaultClubId());
        server.verify();
    }

    @Test
    void verify_shouldReturnDeniedWhenServiceFails() {
        UUID userId = UUID.randomUUID();

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpWorkerAccessVerifier verifier = new HttpWorkerAccessVerifier(builder.build(), INTERNAL_API_KEY);

        server.expect(once(), requestTo(BASE_URL + "/users/" + userId + "/worker-access"))
                .andExpect(method(GET))
                .andExpect(header("x-internal-api-key", INTERNAL_API_KEY))
                .andRespond(withServerError());

        WorkerAccess access = verifier.verify(userId);

        assertFalse(access.hasAccess());
        assertNull(access.defaultClubId());
        server.verify();
    }
}
