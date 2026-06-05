package com.example.authService.integration.infrastructure.controllers;

import com.example.authService.application.dto.AuthResponse;
import com.example.authService.application.dto.ListSessionsRequest;
import com.example.authService.application.dto.LogoutRequest;
import com.example.authService.application.dto.SessionSummary;
import com.example.authService.application.dto.SignInRequest;
import com.example.authService.application.dto.SignUpRequest;
import com.example.authService.application.dto.UpdateDataConsentResponse;
import com.example.authService.application.dto.VerificationEmailRequest;
import com.example.authService.application.dto.VerifyPasswordOtpRequest;
import com.example.authService.application.dto.VerifyPasswordOtpResponse;
import com.example.authService.application.ports.security.ApiClient;
import com.example.authService.application.ports.security.ClientVerifier;
import com.example.authService.application.ports.security.TokenService;
import com.example.authService.application.useCases.ChangePasswordUseCase;
import com.example.authService.application.useCases.DeleteUserUseCase;
import com.example.authService.application.useCases.ListSessionsUseCase;
import com.example.authService.application.useCases.LogoutUseCase;
import com.example.authService.application.useCases.RefreshSessionUseCase;
import com.example.authService.application.useCases.RequestPasswordResetUseCase;
import com.example.authService.application.useCases.ResendVerificationEmailUseCase;
import com.example.authService.application.useCases.ResetPasswordUseCase;
import com.example.authService.application.useCases.RevokeAllSessionsUseCase;
import com.example.authService.application.useCases.RevokeSessionUseCase;
import com.example.authService.application.useCases.SendVerificationEmailUseCase;
import com.example.authService.application.useCases.SignInUseCase;
import com.example.authService.application.useCases.SignUpUseCase;
import com.example.authService.application.useCases.UpdateDataConsentUseCase;
import com.example.authService.application.useCases.VerifyEmailUseCase;
import com.example.authService.application.useCases.VerifyPasswordOtpUseCase;
import com.example.authService.domain.entity.Authentication;
import com.example.authService.domain.entity.Session;
import com.example.authService.domain.enums.SessionStatus;
import com.example.authService.domain.enums.UserRole;
import com.example.authService.domain.exceptions.InvalidCredentialsException;
import com.example.authService.domain.repository.SessionRepository;
import com.example.authService.infrastructure.config.HttpSecurityConfig;
import com.example.authService.infrastructure.controllers.AuthController;
import com.example.authService.infrastructure.controllers.HttpExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({HttpSecurityConfig.class, HttpExceptionHandler.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000",
        "app.cookies.secure=false",
        "app.cookies.same-site=Lax",
        "app.cookies.domain=",
        "app.cookies.access-max-age-seconds=900",
        "app.cookies.refresh-max-age-seconds=604800",
        "app.cookies.reset-max-age-seconds=300"
})
public class AuthControllerIntegrationTest {
    private static final String WEB_API_KEY = "web-key";
    private static final String MOBILE_API_KEY = "mobile-key";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String RESET_TOKEN = "reset-token";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    SignUpUseCase signUpUseCase;

    @MockitoBean
    SignInUseCase signInUseCase;

    @MockitoBean
    RefreshSessionUseCase refreshSessionUseCase;

    @MockitoBean
    LogoutUseCase logoutUseCase;

    @MockitoBean
    SendVerificationEmailUseCase sendVerificationEmailUseCase;

    @MockitoBean
    ResendVerificationEmailUseCase resendVerificationEmailUseCase;

    @MockitoBean
    VerifyEmailUseCase verifyEmailUseCase;

    @MockitoBean
    RequestPasswordResetUseCase requestPasswordResetUseCase;

    @MockitoBean
    VerifyPasswordOtpUseCase verifyPasswordOtpUseCase;

    @MockitoBean
    ResetPasswordUseCase resetPasswordUseCase;

    @MockitoBean
    ChangePasswordUseCase changePasswordUseCase;

    @MockitoBean
    DeleteUserUseCase deleteUserUseCase;

    @MockitoBean
    UpdateDataConsentUseCase updateDataConsentUseCase;

    @MockitoBean
    ListSessionsUseCase listSessionsUseCase;

    @MockitoBean
    RevokeSessionUseCase revokeSessionUseCase;

    @MockitoBean
    RevokeAllSessionsUseCase revokeAllSessionsUseCase;

    @MockitoBean
    ClientVerifier clientVerifier;

    @MockitoBean
    TokenService tokenService;

    @MockitoBean
    SessionRepository sessionRepository;

    @Test
    void signUp_shouldRequireApiKey() throws Exception {
        when(this.clientVerifier.verify(null))
                .thenThrow(new InvalidCredentialsException());

        this.mvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.json(Map.of(
                                "name", "Test User",
                                "email", "test@test.com",
                                "password", "StrongPass1!",
                                "terms", true,
                                "termsVersion", "v1",
                                "commercial", false
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"Invalid API key\"}"));
    }

    @Test
    void signUp_shouldReturnCreatedAndSendVerificationEmailWhenApiKeyIsValid() throws Exception {
        Authentication auth = Authentication.create("test@test.com", "hash", UserRole.MEMBER);
        when(this.clientVerifier.verify(MOBILE_API_KEY))
                .thenReturn(new ApiClient("mobile", UserRole.MEMBER));
        when(this.signUpUseCase.execute(any(SignUpRequest.class), any()))
                .thenReturn(auth);

        this.mvc.perform(post("/auth/signup")
                        .header("x-api-key", MOBILE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.json(Map.of(
                                "name", "Test User",
                                "email", "test@test.com",
                                "password", "StrongPass1!",
                                "terms", true,
                                "termsVersion", "v1",
                                "commercial", false
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered. Check your email to verify your account."));

        verify(this.sendVerificationEmailUseCase).execute(any(VerificationEmailRequest.class));
    }

    @Test
    void signUp_shouldReturnCleanValidationErrors() throws Exception {
        when(this.clientVerifier.verify(MOBILE_API_KEY))
                .thenReturn(new ApiClient("mobile", UserRole.MEMBER));

        this.mvc.perform(post("/auth/signup")
                        .header("x-api-key", MOBILE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.json(Map.of(
                                "name", "Test User",
                                "email", "not-an-email",
                                "password", "StrongPass1!",
                                "terms", false,
                                "termsVersion", "v1",
                                "commercial", false
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.terms").value("You must accept the terms and conditions"));
    }

    @Test
    void signIn_shouldSetHttpOnlyCookiesForWebClient() throws Exception {
        when(this.clientVerifier.verify(WEB_API_KEY))
                .thenReturn(new ApiClient("web", UserRole.ADMIN));
        when(this.signInUseCase.execute(any(SignInRequest.class), any()))
                .thenReturn(new AuthResponse(ACCESS_TOKEN, REFRESH_TOKEN, Optional.empty()));

        this.mvc.perform(post("/auth/signin")
                        .header("x-api-key", WEB_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.json(Map.of(
                                "email", "test@test.com",
                                "password", "StrongPass1!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(cookie().value("token", ACCESS_TOKEN))
                .andExpect(cookie().httpOnly("token", true))
                .andExpect(cookie().value("refreshToken", REFRESH_TOKEN))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.hasItems(
                        org.hamcrest.Matchers.containsString("SameSite=Lax"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax")
                )));
    }

    @Test
    void signIn_shouldReturnTokensInBodyForMobileClient() throws Exception {
        when(this.clientVerifier.verify(MOBILE_API_KEY))
                .thenReturn(new ApiClient("mobile", UserRole.MEMBER));
        when(this.signInUseCase.execute(any(SignInRequest.class), any()))
                .thenReturn(new AuthResponse(ACCESS_TOKEN, REFRESH_TOKEN, Optional.empty()));

        this.mvc.perform(post("/auth/signin")
                        .header("x-api-key", MOBILE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.json(Map.of(
                                "email", "test@test.com",
                                "password", "StrongPass1!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(ACCESS_TOKEN))
                .andExpect(jsonPath("$.refreshToken").value(REFRESH_TOKEN))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void signIn_shouldReturnTooManyRequestsWhenRateLimitIsExceeded() throws Exception {
        when(this.clientVerifier.verify(MOBILE_API_KEY))
                .thenReturn(new ApiClient("mobile", UserRole.MEMBER));
        when(this.signInUseCase.execute(any(SignInRequest.class), any()))
                .thenReturn(new AuthResponse(ACCESS_TOKEN, REFRESH_TOKEN, Optional.empty()));

        for (int i = 0; i < 5; i++) {
            this.mvc.perform(post("/auth/signin")
                            .header("x-api-key", MOBILE_API_KEY)
                            .header("x-forwarded-for", "203.0.113.10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(this.json(Map.of(
                                    "email", "test@test.com",
                                    "password", "StrongPass1!"
                            ))))
                    .andExpect(status().isOk());
        }

        this.mvc.perform(post("/auth/signin")
                        .header("x-api-key", MOBILE_API_KEY)
                        .header("x-forwarded-for", "203.0.113.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.json(Map.of(
                                "email", "test@test.com",
                                "password", "StrongPass1!"
                        ))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().json("{\"error\":\"Too many requests\"}"));

        verify(this.signInUseCase, times(5)).execute(any(SignInRequest.class), any());
    }

    @Test
    void listSessions_shouldRejectAccessTokenWhenSessionDoesNotExist() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(this.tokenService.verifyAccess(ACCESS_TOKEN)).thenReturn(true);
        when(this.tokenService.getTokenId(ACCESS_TOKEN)).thenReturn(userId);
        when(this.tokenService.getSessionId(ACCESS_TOKEN)).thenReturn(sessionId);
        when(this.sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        this.mvc.perform(get("/auth/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void listSessions_shouldRejectAccessTokenWhenSessionIsRevoked() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Session revokedSession = Session.restore(
                sessionId,
                userId,
                "refresh-hash",
                "web",
                "127.0.0.1",
                "Safari",
                null,
                SessionStatus.REVOKED,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Instant.now()
        );

        when(this.tokenService.verifyAccess(ACCESS_TOKEN)).thenReturn(true);
        when(this.tokenService.getTokenId(ACCESS_TOKEN)).thenReturn(userId);
        when(this.tokenService.getSessionId(ACCESS_TOKEN)).thenReturn(sessionId);
        when(this.sessionRepository.findById(sessionId)).thenReturn(Optional.of(revokedSession));

        this.mvc.perform(get("/auth/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void listSessions_shouldReturnSessionsWhenAccessTokenSessionIsActive() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        Session activeSession = Session.restore(
                sessionId,
                userId,
                "refresh-hash",
                "web",
                "127.0.0.1",
                "Safari",
                null,
                SessionStatus.ACTIVE,
                null,
                now.plusSeconds(3600),
                now
        );

        when(this.tokenService.verifyAccess(ACCESS_TOKEN)).thenReturn(true);
        when(this.tokenService.getTokenId(ACCESS_TOKEN)).thenReturn(userId);
        when(this.tokenService.getSessionId(ACCESS_TOKEN)).thenReturn(sessionId);
        when(this.sessionRepository.findById(sessionId)).thenReturn(Optional.of(activeSession));
        when(this.listSessionsUseCase.execute(eq(new ListSessionsRequest(userId))))
                .thenReturn(List.of(new SessionSummary(sessionId, "web", "127.0.0.1", "Safari", now, now.plusSeconds(3600))));

        this.mvc.perform(get("/auth/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sessionId.toString()))
                .andExpect(jsonPath("$[0].client").value("web"));
    }

    @Test
    void logout_shouldAcceptRefreshTokenFromCookieAndClearAuthCookies() throws Exception {
        this.mvc.perform(post("/auth/logout")
                        .cookie(new Cookie("refreshToken", REFRESH_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"))
                .andExpect(cookie().maxAge("token", 0))
                .andExpect(cookie().maxAge("refreshToken", 0));

        verify(this.logoutUseCase).execute(this.logoutRequestWithRefreshToken(REFRESH_TOKEN));
    }

    @Test
    void verifyPasswordOtp_shouldAcceptEmailAndOtpLikeTypeScriptBackend() throws Exception {
        when(this.clientVerifier.verify(WEB_API_KEY))
                .thenReturn(new ApiClient("web", UserRole.MEMBER));
        when(this.verifyPasswordOtpUseCase.execute(any(VerifyPasswordOtpRequest.class)))
                .thenReturn(new VerifyPasswordOtpResponse(RESET_TOKEN));

        this.mvc.perform(post("/auth/password-reset/verify")
                        .header("x-api-key", WEB_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.json(Map.of(
                                "email", "test@test.com",
                                "otp", "123456"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP verified"))
                .andExpect(cookie().value("resetToken", RESET_TOKEN))
                .andExpect(cookie().path("resetToken", "/api/auth/password-reset/confirm"));

        verify(this.verifyPasswordOtpUseCase)
                .execute(eq(new VerifyPasswordOtpRequest("test@test.com", "123456")));
    }

    @Test
    void resetPassword_shouldReturnBadRequestWhenBodyIsMissing() throws Exception {
        when(this.clientVerifier.verify(MOBILE_API_KEY))
                .thenReturn(new ApiClient("mobile", UserRole.MEMBER));

        this.mvc.perform(post("/auth/reset-password")
                        .header("x-api-key", MOBILE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request body"));
    }

    @Test
    void resetPassword_shouldReturnValidationErrorWhenNewPasswordIsBlank() throws Exception {
        when(this.clientVerifier.verify(MOBILE_API_KEY))
                .thenReturn(new ApiClient("mobile", UserRole.MEMBER));

        this.mvc.perform(post("/auth/reset-password")
                        .header("x-api-key", MOBILE_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.json(Map.of(
                                "newPassword", "",
                                "resetToken", RESET_TOKEN
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.newPassword").exists());
    }

    @Test
    void changePassword_shouldReturnValidationErrorWhenBodyFieldsAreBlank() throws Exception {
        this.stubAuthenticatedSession();

        this.mvc.perform(post("/auth/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.json(Map.of(
                                "oldPassword", "",
                                "newPassword", ""
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.oldPassword").exists())
                .andExpect(jsonPath("$.fields.newPassword").exists());
    }

    @Test
    void updateDataConsent_shouldReturnAcceptedAtWhenAuthenticated() throws Exception {
        this.stubAuthenticatedSession();
        Instant acceptedAt = Instant.parse("2026-06-03T22:00:00Z");
        when(this.updateDataConsentUseCase.execute(any()))
                .thenReturn(new UpdateDataConsentResponse(acceptedAt));

        this.mvc.perform(post("/auth/me/data-consent")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.json(Map.of("accept", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedDataCommercializationAt").value("2026-06-03T22:00:00Z"));
    }

    @Test
    void protectedRoute_shouldReturnJsonUnauthorizedWhenAccessTokenIsMissing() throws Exception {
        this.mvc.perform(get("/auth/sessions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void unknownRoute_shouldReturnJsonForbidden() throws Exception {
        this.stubAuthenticatedSession();

        this.mvc.perform(get("/unknown")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    private LogoutRequest logoutRequestWithRefreshToken(String refreshToken) {
        return org.mockito.ArgumentMatchers.argThat(request -> refreshToken.equals(request.refreshToken()));
    }

    private void stubAuthenticatedSession() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        Session activeSession = Session.restore(
                sessionId,
                userId,
                "refresh-hash",
                "web",
                "127.0.0.1",
                "Safari",
                null,
                SessionStatus.ACTIVE,
                null,
                now.plusSeconds(3600),
                now
        );

        when(this.tokenService.verifyAccess(ACCESS_TOKEN)).thenReturn(true);
        when(this.tokenService.getTokenId(ACCESS_TOKEN)).thenReturn(userId);
        when(this.tokenService.getSessionId(ACCESS_TOKEN)).thenReturn(sessionId);
        when(this.sessionRepository.findById(sessionId)).thenReturn(Optional.of(activeSession));
    }

    private String json(Object value) throws Exception {
        return this.objectMapper.writeValueAsString(value);
    }
}
