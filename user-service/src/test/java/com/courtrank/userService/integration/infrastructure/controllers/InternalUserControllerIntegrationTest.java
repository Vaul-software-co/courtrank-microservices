package com.courtrank.userService.integration.infrastructure.controllers;

import com.courtrank.userService.application.dto.AssertUserActiveResponse;
import com.courtrank.userService.application.dto.InternalUserSummaryResponse;
import com.courtrank.userService.application.dto.UserProfileStatusResponse;
import com.courtrank.userService.application.ports.security.AuthSessionVerifier;
import com.courtrank.userService.application.ports.security.TokenService;
import com.courtrank.userService.application.useCases.AssertUserActiveUseCase;
import com.courtrank.userService.application.useCases.BanUserProfileUseCase;
import com.courtrank.userService.application.useCases.CheckUsernameAvailabilityUseCase;
import com.courtrank.userService.application.useCases.GetInternalUserSummaryUseCase;
import com.courtrank.userService.application.useCases.GetInternalUsersByIdsUseCase;
import com.courtrank.userService.application.useCases.UnbanUserProfileUseCase;
import com.courtrank.userService.domain.enums.UserProfileStatus;
import com.courtrank.userService.infrastructure.config.HttpSecurityConfig;
import com.courtrank.userService.infrastructure.controllers.HttpExceptionHandler;
import com.courtrank.userService.infrastructure.controllers.InternalUserController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalUserController.class)
@Import({HttpSecurityConfig.class, HttpExceptionHandler.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000",
        "app.internal-api-key=internal-key"
})
public class InternalUserControllerIntegrationTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    CheckUsernameAvailabilityUseCase checkUsernameAvailabilityUseCase;

    @MockitoBean
    GetInternalUserSummaryUseCase getInternalUserSummaryUseCase;

    @MockitoBean
    GetInternalUsersByIdsUseCase getInternalUsersByIdsUseCase;

    @MockitoBean
    AssertUserActiveUseCase assertUserActiveUseCase;

    @MockitoBean
    BanUserProfileUseCase banUserProfileUseCase;

    @MockitoBean
    UnbanUserProfileUseCase unbanUserProfileUseCase;

    @MockitoBean
    TokenService tokenService;

    @MockitoBean
    AuthSessionVerifier authSessionVerifier;

    @Test
    void internalRoute_shouldRejectMissingApiKey() throws Exception {
        this.mvc.perform(get("/internal/users/username-available")
                        .param("username", "sebas")
                        .param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Invalid internal API key"));
    }

    @Test
    void usernameAvailable_shouldReturnAvailabilityWhenApiKeyMatches() throws Exception {
        when(this.checkUsernameAvailabilityUseCase.execute("sebas", UUID.fromString("4a634d76-d0ac-48d8-9e70-f215094bd19d")))
                .thenReturn(true);

        this.mvc.perform(get("/internal/users/username-available")
                        .header("x-internal-api-key", "internal-key")
                        .param("username", "sebas")
                        .param("userId", "4a634d76-d0ac-48d8-9e70-f215094bd19d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void usernameAvailable_shouldValidateUsername() throws Exception {
        this.mvc.perform(get("/internal/users/username-available")
                        .header("x-internal-api-key", "internal-key")
                        .param("username", "x")
                        .param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));
    }

    @Test
    void summary_shouldReturnInternalSummary() throws Exception {
        UUID id = UUID.randomUUID();
        when(this.getInternalUserSummaryUseCase.execute(any()))
                .thenReturn(new InternalUserSummaryResponse(
                        id,
                        "Sebastian",
                        "sebas",
                        "sebas@test.com",
                        "avatar",
                        false,
                        UserProfileStatus.VISIBLE
                ));

        this.mvc.perform(get("/internal/users/{id}/summary", id)
                        .header("x-internal-api-key", "internal-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("sebas@test.com"));
    }

    @Test
    void summaries_shouldReturnBatchSummaries() throws Exception {
        UUID id = UUID.randomUUID();
        when(this.getInternalUsersByIdsUseCase.execute(any()))
                .thenReturn(List.of(new InternalUserSummaryResponse(
                        id,
                        "Sebastian",
                        "sebas",
                        "sebas@test.com",
                        "avatar",
                        false,
                        UserProfileStatus.VISIBLE
                )));

        this.mvc.perform(post("/internal/users/summaries")
                        .header("x-internal-api-key", "internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.json(Map.of("userIds", List.of(id.toString())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    void active_shouldReturnActiveState() throws Exception {
        UUID id = UUID.randomUUID();
        when(this.assertUserActiveUseCase.execute(any()))
                .thenReturn(new AssertUserActiveResponse(true, UserProfileStatus.VISIBLE));

        this.mvc.perform(get("/internal/users/{id}/active", id)
                        .header("x-internal-api-key", "internal-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void ban_shouldReturnSuspendedStatus() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        when(this.banUserProfileUseCase.execute(any(), any()))
                .thenReturn(new UserProfileStatusResponse(UserProfileStatus.SUSPENDED));

        this.mvc.perform(post("/internal/users/{id}/ban", targetUserId)
                        .header("x-internal-api-key", "internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.json(Map.of("adminUserId", adminUserId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void unban_shouldReturnVisibleStatus() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        when(this.unbanUserProfileUseCase.execute(any(), any()))
                .thenReturn(new UserProfileStatusResponse(UserProfileStatus.VISIBLE));

        this.mvc.perform(post("/internal/users/{id}/unban", targetUserId)
                        .header("x-internal-api-key", "internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.json(Map.of("adminUserId", adminUserId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VISIBLE"));
    }

    private String json(Object value) throws Exception {
        return this.objectMapper.writeValueAsString(value);
    }
}
