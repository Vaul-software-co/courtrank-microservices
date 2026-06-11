package com.courtrank.userService.unit.infrastructure.events;

import com.courtrank.userService.application.dto.CreateUserRequest;
import com.courtrank.userService.application.dto.RestoreUserRequest;
import com.courtrank.userService.application.useCases.CreateUserFromAuthEventUseCase;
import com.courtrank.userService.application.useCases.DeleteUserFromAuthEventUseCase;
import com.courtrank.userService.application.useCases.MarkUserEmailVerifiedFromAuthEventUseCase;
import com.courtrank.userService.application.useCases.RestoreUserFromAuthEventUseCase;
import com.courtrank.userService.infrastructure.events.AuthEventConsumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AuthEventConsumerTest {
    @Test
    void consume_shouldCreateUserWhenAuthEventIsUserRegistered() throws Exception {
        FakeCreateUserFromAuthEventUseCase createUseCase = new FakeCreateUserFromAuthEventUseCase();
        FakeRestoreUserFromAuthEventUseCase restoreUseCase = new FakeRestoreUserFromAuthEventUseCase();
        FakeDeleteUserFromAuthEventUseCase deleteUseCase = new FakeDeleteUserFromAuthEventUseCase();
        FakeMarkUserEmailVerifiedFromAuthEventUseCase verifyUseCase = new FakeMarkUserEmailVerifiedFromAuthEventUseCase();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AuthEventConsumer consumer = new AuthEventConsumer(objectMapper, createUseCase, restoreUseCase, deleteUseCase, verifyUseCase);
        UUID userId = UUID.randomUUID();

        String message = objectMapper.writeValueAsString(Map.of(
                "eventId", UUID.randomUUID(),
                "eventType", "USER_REGISTERED",
                "aggregateId", userId,
                "source", "auth-service",
                "payload", Map.of(
                        "id", userId,
                        "email", "test@test.com",
                        "name", "Test User",
                        "username", "testuser",
                        "emailVerified", false
                ),
                "publishedAt", Instant.parse("2026-01-01T00:00:00Z")
        ));

        consumer.consume(message);

        CreateUserRequest request = createUseCase.request;
        assertEquals(userId, request.id());
        assertEquals("test@test.com", request.email());
        assertEquals("Test User", request.name());
        assertEquals("testuser", request.userName());
        assertEquals(false, request.emailVerified());
        assertNull(restoreUseCase.request);
        assertNull(deleteUseCase.userId);
    }

    @Test
    void consume_shouldRestoreUserWhenAuthEventIsUserRestoredWithoutUsername() throws Exception {
        FakeCreateUserFromAuthEventUseCase createUseCase = new FakeCreateUserFromAuthEventUseCase();
        FakeRestoreUserFromAuthEventUseCase restoreUseCase = new FakeRestoreUserFromAuthEventUseCase();
        FakeDeleteUserFromAuthEventUseCase deleteUseCase = new FakeDeleteUserFromAuthEventUseCase();
        FakeMarkUserEmailVerifiedFromAuthEventUseCase verifyUseCase = new FakeMarkUserEmailVerifiedFromAuthEventUseCase();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AuthEventConsumer consumer = new AuthEventConsumer(objectMapper, createUseCase, restoreUseCase, deleteUseCase, verifyUseCase);
        UUID userId = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", userId);
        payload.put("email", "test@test.com");
        payload.put("name", "Test User");
        payload.put("emailVerified", true);

        String message = objectMapper.writeValueAsString(Map.of(
                "eventId", UUID.randomUUID(),
                "eventType", "USER_RESTORED",
                "aggregateId", userId,
                "source", "auth-service",
                "payload", payload,
                "publishedAt", Instant.parse("2026-01-01T00:00:00Z")
        ));

        consumer.consume(message);

        RestoreUserRequest request = restoreUseCase.request;
        assertEquals(userId, request.id());
        assertEquals("test@test.com", request.email());
        assertEquals("Test User", request.name());
        assertNull(request.userName());
        assertEquals(true, request.emailVerified());
        assertNull(createUseCase.request);
        assertNull(deleteUseCase.userId);
    }

    @Test
    void consume_shouldDeleteUserWhenAuthEventIsUserDeleted() throws Exception {
        FakeCreateUserFromAuthEventUseCase createUseCase = new FakeCreateUserFromAuthEventUseCase();
        FakeRestoreUserFromAuthEventUseCase restoreUseCase = new FakeRestoreUserFromAuthEventUseCase();
        FakeDeleteUserFromAuthEventUseCase deleteUseCase = new FakeDeleteUserFromAuthEventUseCase();
        FakeMarkUserEmailVerifiedFromAuthEventUseCase verifyUseCase = new FakeMarkUserEmailVerifiedFromAuthEventUseCase();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AuthEventConsumer consumer = new AuthEventConsumer(objectMapper, createUseCase, restoreUseCase, deleteUseCase, verifyUseCase);
        UUID userId = UUID.randomUUID();

        String message = objectMapper.writeValueAsString(Map.of(
                "eventId", UUID.randomUUID(),
                "eventType", "USER_DELETED",
                "aggregateId", userId,
                "source", "auth-service",
                "payload", Map.of(
                        "id", userId,
                        "email", "test@test.com"
                ),
                "publishedAt", Instant.parse("2026-01-01T00:00:00Z")
        ));

        consumer.consume(message);

        assertNull(createUseCase.request);
        assertNull(restoreUseCase.request);
        assertEquals(userId, deleteUseCase.userId);
    }

    @Test
    void consume_shouldMarkEmailVerifiedWhenAuthEventIsUserEmailVerified() throws Exception {
        FakeCreateUserFromAuthEventUseCase createUseCase = new FakeCreateUserFromAuthEventUseCase();
        FakeRestoreUserFromAuthEventUseCase restoreUseCase = new FakeRestoreUserFromAuthEventUseCase();
        FakeDeleteUserFromAuthEventUseCase deleteUseCase = new FakeDeleteUserFromAuthEventUseCase();
        FakeMarkUserEmailVerifiedFromAuthEventUseCase verifyUseCase = new FakeMarkUserEmailVerifiedFromAuthEventUseCase();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AuthEventConsumer consumer = new AuthEventConsumer(objectMapper, createUseCase, restoreUseCase, deleteUseCase, verifyUseCase);
        UUID userId = UUID.randomUUID();

        String message = objectMapper.writeValueAsString(Map.of(
                "eventId", UUID.randomUUID(),
                "eventType", "USER_EMAIL_VERIFIED",
                "aggregateId", userId,
                "source", "auth-service",
                "payload", Map.of(
                        "id", userId,
                        "email", "test@test.com"
                ),
                "publishedAt", Instant.parse("2026-01-01T00:00:00Z")
        ));

        consumer.consume(message);

        assertEquals(userId, verifyUseCase.userId);
        assertNull(createUseCase.request);
        assertNull(restoreUseCase.request);
        assertNull(deleteUseCase.userId);
    }

    private static class FakeCreateUserFromAuthEventUseCase extends CreateUserFromAuthEventUseCase {
        private CreateUserRequest request;

        private FakeCreateUserFromAuthEventUseCase() {
            super(null, null, null);
        }

        @Override
        public void execute(CreateUserRequest request) {
            this.request = request;
        }
    }

    private static class FakeRestoreUserFromAuthEventUseCase extends RestoreUserFromAuthEventUseCase {
        private RestoreUserRequest request;

        private FakeRestoreUserFromAuthEventUseCase() {
            super(null, null);
        }

        @Override
        public void execute(RestoreUserRequest request) {
            this.request = request;
        }
    }

    private static class FakeDeleteUserFromAuthEventUseCase extends DeleteUserFromAuthEventUseCase {
        private UUID userId;

        private FakeDeleteUserFromAuthEventUseCase() {
            super(null, null);
        }

        @Override
        public void execute(UUID userId) {
            this.userId = userId;
        }
    }

    private static class FakeMarkUserEmailVerifiedFromAuthEventUseCase extends MarkUserEmailVerifiedFromAuthEventUseCase {
        private UUID userId;

        private FakeMarkUserEmailVerifiedFromAuthEventUseCase() {
            super(null, null);
        }

        @Override
        public void execute(UUID userId) {
            this.userId = userId;
        }
    }
}
