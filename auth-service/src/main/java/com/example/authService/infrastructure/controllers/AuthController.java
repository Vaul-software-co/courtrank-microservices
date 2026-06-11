package com.example.authService.infrastructure.controllers;

import com.example.authService.application.dto.AuthResponse;
import com.example.authService.application.dto.ChangePasswordRequest;
import com.example.authService.application.dto.DeleteUserRequest;
import com.example.authService.application.dto.HttpContext;
import com.example.authService.application.dto.ListSessionsRequest;
import com.example.authService.application.dto.LogoutRequest;
import com.example.authService.application.dto.RefreshSessionRequest;
import com.example.authService.application.dto.RequestPasswordResetRequest;
import com.example.authService.application.dto.ResendVerificationEmailRequest;
import com.example.authService.application.dto.ResetPasswordRequest;
import com.example.authService.application.dto.RevokeAllSessionsRequest;
import com.example.authService.application.dto.RevokeSessionRequest;
import com.example.authService.application.dto.SessionSummary;
import com.example.authService.application.dto.SignInRequest;
import com.example.authService.application.dto.SignUpRequest;
import com.example.authService.application.dto.SignUpResponse;
import com.example.authService.application.dto.UpdateDataConsentRequest;
import com.example.authService.application.dto.UpdateDataConsentResponse;
import com.example.authService.application.dto.VerificationEmailRequest;
import com.example.authService.application.dto.VerifyEmailRequest;
import com.example.authService.application.dto.VerifyPasswordOtpRequest;
import com.example.authService.application.dto.VerifyPasswordOtpResponse;
import com.example.authService.application.ports.security.ApiClient;
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
import com.example.authService.domain.exceptions.InvalidCredentialsException;
import com.example.authService.infrastructure.config.CookieProperties;
import com.example.authService.infrastructure.security.ApiKeyFilter;
import com.example.authService.infrastructure.security.AuthUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final String ACCESS_TOKEN_COOKIE = "token";
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String RESET_TOKEN_COOKIE = "resetToken";
    private static final String RESET_TOKEN_COOKIE_PATH = "/api/auth/password-reset/confirm";

    private final SignUpUseCase signUpUseCase;
    private final SignInUseCase signInUseCase;
    private final RefreshSessionUseCase refreshSessionUseCase;
    private final LogoutUseCase logoutUseCase;
    private final SendVerificationEmailUseCase sendVerificationEmailUseCase;
    private final ResendVerificationEmailUseCase resendVerificationEmailUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final VerifyPasswordOtpUseCase verifyPasswordOtpUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final UpdateDataConsentUseCase updateDataConsentUseCase;
    private final ListSessionsUseCase listSessionsUseCase;
    private final RevokeSessionUseCase revokeSessionUseCase;
    private final RevokeAllSessionsUseCase revokeAllSessionsUseCase;
    private final CookieProperties cookieProperties;

    public AuthController(
            SignUpUseCase signUpUseCase,
            SignInUseCase signInUseCase,
            RefreshSessionUseCase refreshSessionUseCase,
            LogoutUseCase logoutUseCase,
            SendVerificationEmailUseCase sendVerificationEmailUseCase,
            ResendVerificationEmailUseCase resendVerificationEmailUseCase,
            VerifyEmailUseCase verifyEmailUseCase,
            RequestPasswordResetUseCase requestPasswordResetUseCase,
            VerifyPasswordOtpUseCase verifyPasswordOtpUseCase,
            ResetPasswordUseCase resetPasswordUseCase,
            ChangePasswordUseCase changePasswordUseCase,
            DeleteUserUseCase deleteUserUseCase,
            UpdateDataConsentUseCase updateDataConsentUseCase,
            ListSessionsUseCase listSessionsUseCase,
            RevokeSessionUseCase revokeSessionUseCase,
            RevokeAllSessionsUseCase revokeAllSessionsUseCase,
            CookieProperties cookieProperties
    ) {
        this.signUpUseCase = signUpUseCase;
        this.signInUseCase = signInUseCase;
        this.refreshSessionUseCase = refreshSessionUseCase;
        this.logoutUseCase = logoutUseCase;
        this.sendVerificationEmailUseCase = sendVerificationEmailUseCase;
        this.resendVerificationEmailUseCase = resendVerificationEmailUseCase;
        this.verifyEmailUseCase = verifyEmailUseCase;
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.verifyPasswordOtpUseCase = verifyPasswordOtpUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.updateDataConsentUseCase = updateDataConsentUseCase;
        this.listSessionsUseCase = listSessionsUseCase;
        this.revokeSessionUseCase = revokeSessionUseCase;
        this.revokeAllSessionsUseCase = revokeAllSessionsUseCase;
        this.cookieProperties = cookieProperties;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> signUp(
            @Valid @RequestBody SignUpRequest request,
            HttpServletRequest servletRequest
    ) {
        HttpContext http = this.httpContext(servletRequest);
        SignUpResponse response = this.signUpUseCase.execute(request, http);
        Authentication auth = response.authentication();
        this.sendVerificationEmailUseCase.execute(new VerificationEmailRequest(auth.getId(), auth.getEmail(), null));

        if (response.auth().isPresent()) {
            return this.tokenResponse(response.auth().orElseThrow());
        }

        return Map.of("message", "User registered. Check your email to verify your account.");
    }

    @PostMapping("/signin")
    public Map<String, Object> signIn(
            @Valid @RequestBody SignInRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        HttpContext http = this.httpContext(servletRequest);
        AuthResponse response = this.signInUseCase.execute(request, http);

        if (this.isWeb(http)) {
            this.setAuthCookies(servletResponse, response.accessToken(), response.refreshToken());
            return this.withOptionalClubId("Login successful", response);
        }

        return this.tokenResponse(response);
    }

    @PostMapping("/refresh")
    public Map<String, Object> refresh(
            @RequestBody(required = false) RefreshTokenBody body,
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshCookie,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        HttpContext http = this.httpContext(servletRequest);
        String refreshToken = this.requiredToken(refreshCookie, body == null ? null : body.refreshToken());

        AuthResponse response = this.refreshSessionUseCase.execute(new RefreshSessionRequest(refreshToken, http));
        if (this.isWeb(http)) {
            this.setAuthCookies(servletResponse, response.accessToken(), response.refreshToken());
            return Map.of("message", "Login successful");
        }

        return this.tokenResponse(response);
    }

    @RequestMapping(value = "/logout", method = {RequestMethod.POST, RequestMethod.DELETE})
    public Map<String, String> logout(
            @RequestBody(required = false) RefreshTokenBody body,
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshCookie,
            @RequestParam(name = REFRESH_TOKEN_COOKIE, required = false) String refreshQuery,
            HttpServletResponse servletResponse
    ) {
        String refreshToken = this.requiredToken(refreshCookie, body == null ? refreshQuery : this.firstNonBlank(body.refreshToken(), refreshQuery));
        this.logoutUseCase.execute(new LogoutRequest(refreshToken));
        this.clearAuthCookies(servletResponse);

        return Map.of("message", "Logout successful");
    }

    @PostMapping({"/resend-verification-email", "/verify-email/resend"})
    public Map<String, String> resendVerificationEmail(
            @Valid @RequestBody ResendVerificationEmailRequest request
    ) {
        this.resendVerificationEmailUseCase.execute(request);
        return Map.of("message", "Verification email sent");
    }

    @PostMapping({"/verify-email", "/verify-email/confirm"})
    public Map<String, String> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        this.verifyEmailUseCase.execute(request);
        return Map.of("message", "Email verified");
    }

    @PostMapping({"/request-password-reset", "/password-reset/request"})
    public Map<String, String> requestPasswordReset(
            @Valid @RequestBody RequestPasswordResetRequest request
    ) {
        this.requestPasswordResetUseCase.execute(request);
        return Map.of("message", "If the email exists, you will receive a code");
    }

    @PostMapping({"/verify-password-otp", "/password-reset/verify"})
    public Map<String, String> verifyPasswordOtp(
            @Valid @RequestBody VerifyPasswordOtpRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        ApiClient client = this.apiClient(servletRequest);
        VerifyPasswordOtpResponse response = this.verifyPasswordOtpUseCase.execute(request);
        this.setResetTokenCookie(servletResponse, response.resetToken());

        if ("web".equals(client.client())) {
            return Map.of("message", "OTP verified");
        }

        return Map.of("message", "OTP verified", "resetToken", response.resetToken());
    }

    @RequestMapping(value = {"/reset-password", "/password-reset/confirm"}, method = {RequestMethod.POST, RequestMethod.PUT})
    public Map<String, String> resetPassword(
            @Valid @RequestBody ResetPasswordBody body,
            @CookieValue(name = RESET_TOKEN_COOKIE, required = false) String resetCookie,
            HttpServletRequest servletRequest
    ) {
        this.apiClient(servletRequest);
        String resetToken = this.requiredToken(resetCookie, body.resetToken());
        this.resetPasswordUseCase.execute(new ResetPasswordRequest(resetToken, body.newPassword()));
        return Map.of("message", "Password updated successfully");
    }

    @PostMapping("/change-password")
    public Map<String, String> changePassword(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody ChangePasswordBody body
    ) {
        this.changePasswordUseCase.execute(new ChangePasswordRequest(
                principal.userId(),
                body.oldPassword(),
                body.newPassword()
        ));

        return Map.of("message", "Password updated successfully");
    }

    @DeleteMapping("/me")
    public Map<String, String> deleteMe(@AuthenticationPrincipal AuthUserPrincipal principal) {
        this.deleteUserUseCase.execute(new DeleteUserRequest(principal.userId()));
        return Map.of("message", "User deleted");
    }

    @PostMapping("/me/data-consent")
    public UpdateDataConsentResponse updateDataConsent(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody DataConsentBody body
    ) {
        return this.updateDataConsentUseCase.execute(new UpdateDataConsentRequest(
                principal.userId(),
                body.accept()
        ));
    }

    @GetMapping("/sessions")
    public List<SessionSummary> listSessions(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return this.listSessionsUseCase.execute(new ListSessionsRequest(principal.userId()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Map<String, String> revokeSession(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable UUID sessionId
    ) {
        this.revokeSessionUseCase.execute(new RevokeSessionRequest(principal.userId(), sessionId));
        return Map.of("message", "Session revoked");
    }

    @DeleteMapping("/sessions")
    public Map<String, String> revokeAllSessions(@AuthenticationPrincipal AuthUserPrincipal principal) {
        this.revokeAllSessionsUseCase.execute(new RevokeAllSessionsRequest(principal.userId()));
        return Map.of("message", "Sessions revoked");
    }

    private HttpContext httpContext(HttpServletRequest request) {
        ApiClient client = this.apiClient(request);
        return new HttpContext(
                client.client(),
                this.ip(request),
                request.getHeader("User-Agent"),
                client.type(),
                request.getHeader("x-request-id")
        );
    }

    private ApiClient apiClient(HttpServletRequest request) {
        Object client = request.getAttribute(ApiKeyFilter.API_CLIENT_ATTRIBUTE);
        if (client instanceof ApiClient apiClient) {
            return apiClient;
        }

        throw new InvalidCredentialsException();
    }

    private String ip(HttpServletRequest request) {
        String forwardedFor = request.getHeader("x-forwarded-for");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private boolean isWeb(HttpContext http) {
        return "web".equals(http.client());
    }

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, this.cookie(
                ACCESS_TOKEN_COOKIE,
                accessToken,
                Duration.ofSeconds(this.cookieProperties.accessMaxAgeSeconds()),
                "/"
        ).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, this.cookie(
                REFRESH_TOKEN_COOKIE,
                refreshToken,
                Duration.ofSeconds(this.cookieProperties.refreshMaxAgeSeconds()),
                "/"
        ).toString());
    }

    private void setResetTokenCookie(HttpServletResponse response, String resetToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, this.cookie(
                RESET_TOKEN_COOKIE,
                resetToken,
                Duration.ofSeconds(this.cookieProperties.resetMaxAgeSeconds()),
                RESET_TOKEN_COOKIE_PATH
        ).toString());
    }

    private void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, this.cookie(ACCESS_TOKEN_COOKIE, "", Duration.ZERO, "/").toString());
        response.addHeader(HttpHeaders.SET_COOKIE, this.cookie(REFRESH_TOKEN_COOKIE, "", Duration.ZERO, "/").toString());
    }

    private ResponseCookie cookie(String name, String value, Duration maxAge, String path) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(this.cookieProperties.secure())
                .sameSite(this.cookieProperties.sameSite())
                .path(path)
                .maxAge(maxAge);

        if (this.cookieProperties.hasDomain()) {
            builder.domain(this.cookieProperties.domain());
        }

        return builder.build();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        return second;
    }

    private String requiredToken(String first, String second) {
        String token = this.firstNonBlank(first, second);
        if (token == null || token.isBlank()) {
            throw new InvalidCredentialsException();
        }

        return token;
    }

    private Map<String, Object> tokenResponse(AuthResponse response) {
        return Map.of(
                "token", response.accessToken(),
                "refreshToken", response.refreshToken()
        );
    }

    private Map<String, Object> withOptionalClubId(String message, AuthResponse response) {
        if (response.clubId().isPresent()) {
            return Map.of("message", message, "clubId", response.clubId().orElseThrow());
        }

        return Map.of("message", message);
    }

    public record RefreshTokenBody(String refreshToken) {
    }

    public record ResetPasswordBody(
            @NotBlank
            String newPassword,

            String resetToken
    ) {
    }

    public record ChangePasswordBody(
            @NotBlank
            String oldPassword,

            @NotBlank
            String newPassword
    ) {
    }

    public record DataConsentBody(boolean accept) {
    }
}
