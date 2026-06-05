package com.example.userService.integration.infrastructure.controllers;

import com.example.userService.application.dto.MyProfileResponse;
import com.example.userService.application.dto.PublicProfileResponse;
import com.example.userService.application.dto.RemoveMyAvatarResponse;
import com.example.userService.application.dto.UpdateMyAvatarResponse;
import com.example.userService.application.dto.UpdateMyLangResponse;
import com.example.userService.application.dto.UpdateMyPrivacyResponse;
import com.example.userService.application.dto.UserSearchResult;
import com.example.userService.application.ports.security.AuthSessionVerifier;
import com.example.userService.application.ports.security.TokenService;
import com.example.userService.application.useCases.GetMyProfileUseCase;
import com.example.userService.application.useCases.GetUserPublicProfileUseCase;
import com.example.userService.application.useCases.RemoveMyAvatarUseCase;
import com.example.userService.application.useCases.SearchUsersUseCase;
import com.example.userService.application.useCases.UpdateMyAvatarUseCase;
import com.example.userService.application.useCases.UpdateMyLangUseCase;
import com.example.userService.application.useCases.UpdateMyPrivacyUseCase;
import com.example.userService.application.useCases.UpdateMyProfileUseCase;
import com.example.userService.domain.enums.UserGender;
import com.example.userService.domain.enums.UserProfileStatus;
import com.example.userService.domain.exceptions.UserNameAlreadyTakenException;
import com.example.userService.infrastructure.config.HttpSecurityConfig;
import com.example.userService.infrastructure.controllers.HttpExceptionHandler;
import com.example.userService.infrastructure.controllers.UserController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({HttpSecurityConfig.class, HttpExceptionHandler.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000",
        "app.internal-api-key=internal-key"
})
public class UserControllerIntegrationTest {
    private static final String ACCESS_TOKEN = "access-token";
    private static final UUID USER_ID = UUID.fromString("8d7107e8-571d-4a15-a64d-2823dc4731ff");
    private static final UUID SESSION_ID = UUID.fromString("640b1542-42e7-4458-80db-8dfaa255b5e2");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    GetMyProfileUseCase getMyProfileUseCase;

    @MockitoBean
    UpdateMyProfileUseCase updateMyProfileUseCase;

    @MockitoBean
    UpdateMyPrivacyUseCase updateMyPrivacyUseCase;

    @MockitoBean
    UpdateMyLangUseCase updateMyLangUseCase;

    @MockitoBean
    GetUserPublicProfileUseCase getUserPublicProfileUseCase;

    @MockitoBean
    SearchUsersUseCase searchUsersUseCase;

    @MockitoBean
    UpdateMyAvatarUseCase updateMyAvatarUseCase;

    @MockitoBean
    RemoveMyAvatarUseCase removeMyAvatarUseCase;

    @MockitoBean
    TokenService tokenService;

    @MockitoBean
    AuthSessionVerifier authSessionVerifier;

    @Test
    void me_shouldReturnProfileWhenTokenSessionIsActive() throws Exception {
        this.stubAuthenticatedSession();
        when(this.getMyProfileUseCase.execute(any(), any()))
                .thenReturn(new MyProfileResponse(
                        USER_ID,
                        "Sebastian",
                        "sebas",
                        "sebas@test.com",
                        "+573001112233",
                        UserGender.MALE,
                        "avatar-key",
                        false,
                        UserProfileStatus.VISIBLE,
                        "es",
                        Instant.parse("2026-01-01T00:00:00Z")
                ));

        this.mvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, this.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("sebas@test.com"));
    }

    @Test
    void protectedRoute_shouldReturnUnauthorizedWhenTokenIsMissing() throws Exception {
        this.mvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void protectedRoute_shouldReturnUnauthorizedWhenSessionIsInactive() throws Exception {
        when(this.tokenService.verifyAccess(ACCESS_TOKEN)).thenReturn(true);
        when(this.tokenService.getTokenId(ACCESS_TOKEN)).thenReturn(USER_ID);
        when(this.tokenService.getSessionId(ACCESS_TOKEN)).thenReturn(SESSION_ID);
        when(this.authSessionVerifier.isActive(SESSION_ID)).thenReturn(false);

        this.mvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, this.bearer()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void updateMe_shouldReturnConflictWhenUsernameIsTaken() throws Exception {
        this.stubAuthenticatedSession();
        when(this.updateMyProfileUseCase.execute(any(), any()))
                .thenThrow(new UserNameAlreadyTakenException());

        this.mvc.perform(patch("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, this.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.json(Map.of("username", "taken"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void updatePrivacy_shouldValidateRequiredFlag() throws Exception {
        this.stubAuthenticatedSession();

        this.mvc.perform(patch("/users/me/privacy")
                        .header(HttpHeaders.AUTHORIZATION, this.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.privateProfile").exists());
    }

    @Test
    void updateLang_shouldReturnUpdatedLanguage() throws Exception {
        this.stubAuthenticatedSession();
        when(this.updateMyLangUseCase.execute(any(), any()))
                .thenReturn(new UpdateMyLangResponse("en"));

        this.mvc.perform(patch("/users/me/lang")
                        .header(HttpHeaders.AUTHORIZATION, this.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.json(Map.of("lang", "en"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lang").value("en"));
    }

    @Test
    void updateAvatar_shouldReturnStoredAvatarReference() throws Exception {
        this.stubAuthenticatedSession();
        when(this.updateMyAvatarUseCase.execute(any(), any()))
                .thenReturn(new UpdateMyAvatarResponse("avatars/user.png", "avatars/user.png"));

        this.mvc.perform(put("/users/me/avatar")
                        .header(HttpHeaders.AUTHORIZATION, this.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.json(Map.of("avatarKey", "avatars/user.png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarKey").value("avatars/user.png"));
    }

    @Test
    void removeAvatar_shouldReturnEmptyAvatarReference() throws Exception {
        this.stubAuthenticatedSession();
        when(this.removeMyAvatarUseCase.execute(any(), any()))
                .thenReturn(new RemoveMyAvatarResponse(null, null));

        this.mvc.perform(delete("/users/me/avatar").header(HttpHeaders.AUTHORIZATION, this.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").doesNotExist());
    }

    @Test
    void search_shouldValidateQueryLength() throws Exception {
        this.stubAuthenticatedSession();

        this.mvc.perform(get("/users/search")
                        .header(HttpHeaders.AUTHORIZATION, this.bearer())
                        .param("q", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));
    }

    @Test
    void search_shouldReturnResults() throws Exception {
        this.stubAuthenticatedSession();
        UUID targetId = UUID.randomUUID();
        when(this.searchUsersUseCase.execute(any()))
                .thenReturn(List.of(new UserSearchResult(targetId, "Player", "player", "avatar")));

        this.mvc.perform(get("/users/search")
                        .header(HttpHeaders.AUTHORIZATION, this.bearer())
                        .param("q", "pla")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(targetId.toString()))
                .andExpect(jsonPath("$[0].username").value("player"));
    }

    @Test
    void publicProfile_shouldReturnPublicProfile() throws Exception {
        this.stubAuthenticatedSession();
        UUID targetId = UUID.randomUUID();
        when(this.getUserPublicProfileUseCase.execute(any(), any()))
                .thenReturn(new PublicProfileResponse(
                        targetId,
                        "Player",
                        "player",
                        "avatar",
                        false,
                        UserProfileStatus.VISIBLE,
                        Instant.parse("2026-01-01T00:00:00Z")
                ));

        this.mvc.perform(get("/users/{id}", targetId).header(HttpHeaders.AUTHORIZATION, this.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetId.toString()))
                .andExpect(jsonPath("$.username").value("player"));
    }

    private void stubAuthenticatedSession() {
        when(this.tokenService.verifyAccess(ACCESS_TOKEN)).thenReturn(true);
        when(this.tokenService.getTokenId(ACCESS_TOKEN)).thenReturn(USER_ID);
        when(this.tokenService.getSessionId(ACCESS_TOKEN)).thenReturn(SESSION_ID);
        when(this.authSessionVerifier.isActive(SESSION_ID)).thenReturn(true);
    }

    private String bearer() {
        return "Bearer " + ACCESS_TOKEN;
    }

    private String json(Object value) throws Exception {
        return this.objectMapper.writeValueAsString(value);
    }
}
