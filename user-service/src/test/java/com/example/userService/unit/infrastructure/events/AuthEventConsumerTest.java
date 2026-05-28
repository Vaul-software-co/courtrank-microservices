package com.example.userService.unit.infrastructure.events;

import com.example.userService.application.dto.CreateUserRequest;
import com.example.userService.application.useCases.CreateUserFromAuthEventUseCase;
import com.example.userService.infrastructure.events.AuthEventConsumer;
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
        FakeCreateUserFromAuthEventUseCase useCase = new FakeCreateUserFromAuthEventUseCase();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AuthEventConsumer consumer = new AuthEventConsumer(objectMapper, useCase);
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
                        "username", "testuser"
                ),
                "publishedAt", Instant.parse("2026-01-01T00:00:00Z")
        ));

        consumer.consume(message);

        CreateUserRequest request = useCase.request;
        assertEquals(userId, request.id());
        assertEquals("test@test.com", request.email());
        assertEquals("Test User", request.name());
        assertEquals("testuser", request.userName());
    }

    @Test
    void consume_shouldCreateUserWhenAuthEventIsUserRestoredWithoutUsername() throws Exception {
        FakeCreateUserFromAuthEventUseCase useCase = new FakeCreateUserFromAuthEventUseCase();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AuthEventConsumer consumer = new AuthEventConsumer(objectMapper, useCase);
        UUID userId = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", userId);
        payload.put("email", "test@test.com");
        payload.put("name", "Test User");

        String message = objectMapper.writeValueAsString(Map.of(
                "eventId", UUID.randomUUID(),
                "eventType", "USER_RESTORED",
                "aggregateId", userId,
                "source", "auth-service",
                "payload", payload,
                "publishedAt", Instant.parse("2026-01-01T00:00:00Z")
        ));

        consumer.consume(message);

        CreateUserRequest request = useCase.request;
        assertEquals(userId, request.id());
        assertEquals("test@test.com", request.email());
        assertEquals("Test User", request.name());
        assertNull(request.userName());
    }

    @Test
    void consume_shouldIgnoreUnrelatedAuthEvents() throws Exception {
        FakeCreateUserFromAuthEventUseCase useCase = new FakeCreateUserFromAuthEventUseCase();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AuthEventConsumer consumer = new AuthEventConsumer(objectMapper, useCase);
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

        assertNull(useCase.request);
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
}
