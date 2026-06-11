package com.courtrank.authService.unit.application.dto;

import com.courtrank.authService.application.dto.ChangePasswordRequest;
import com.courtrank.authService.application.dto.DeleteUserRequest;
import com.courtrank.authService.application.dto.HttpContext;
import com.courtrank.authService.application.dto.ListSessionsRequest;
import com.courtrank.authService.application.dto.RefreshSessionRequest;
import com.courtrank.authService.application.dto.RequestPasswordResetRequest;
import com.courtrank.authService.application.dto.ResendVerificationEmailRequest;
import com.courtrank.authService.application.dto.ResetPasswordRequest;
import com.courtrank.authService.application.dto.RevokeAllSessionsRequest;
import com.courtrank.authService.application.dto.RevokeSessionRequest;
import com.courtrank.authService.application.dto.SignInRequest;
import com.courtrank.authService.application.dto.SignUpRequest;
import com.courtrank.authService.application.dto.VerificationEmailRequest;
import com.courtrank.authService.application.dto.VerifyEmailRequest;
import com.courtrank.authService.application.dto.VerifyPasswordOtpRequest;
import com.courtrank.authService.domain.enums.UserRole;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DtoValidationTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final String EMAIL = "test@test.com";
    private static final String PASSWORD = "StrongPass1!";
    private static final String TOKEN = "token";
    private static final HttpContext HTTP_CONTEXT = new HttpContext(
            "web",
            "127.0.0.1",
            "Safari",
            UserRole.MEMBER
    );

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private <T> void assertValid(T dto) {
        Set<ConstraintViolation<T>> violations = this.validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    private <T> void assertInvalid(T dto) {
        Set<ConstraintViolation<T>> violations = this.validator.validate(dto);

        assertFalse(violations.isEmpty());
    }

    @Test
    void signUpRequest_shouldBeValidWhenRequiredFieldsAreValidAndTermsAccepted() {
        SignUpRequest request = new SignUpRequest(
                "Test User",
                "test_user",
                EMAIL,
                PASSWORD,
                true,
                "v1",
                false
        );

        this.assertValid(request);
    }

    @Test
    void signUpRequest_shouldBeInvalidWhenTermsAreNotAccepted() {
        SignUpRequest request = new SignUpRequest(
                "Test User",
                "test_user",
                EMAIL,
                PASSWORD,
                false,
                "v1",
                false
        );

        this.assertInvalid(request);
    }

    @Test
    void signUpRequest_shouldBeInvalidWhenEmailIsInvalid() {
        SignUpRequest request = new SignUpRequest(
                "Test User",
                "test_user",
                "invalid-email",
                PASSWORD,
                true,
                "v1",
                false
        );

        this.assertInvalid(request);
    }

    @Test
    void signUpRequest_shouldBeInvalidWhenRequiredTextFieldsAreBlank() {
        SignUpRequest request = new SignUpRequest(
                "",
                "",
                "",
                "",
                true,
                "",
                false
        );

        this.assertInvalid(request);
    }

    @Test
    void signInRequest_shouldBeValidWhenEmailAndPasswordArePresent() {
        this.assertValid(new SignInRequest(EMAIL, PASSWORD));
    }

    @Test
    void signInRequest_shouldBeInvalidWhenEmailIsBlank() {
        this.assertInvalid(new SignInRequest("", PASSWORD));
    }

    @Test
    void signInRequest_shouldBeInvalidWhenEmailFormatIsInvalid() {
        this.assertInvalid(new SignInRequest("invalid-email", PASSWORD));
    }

    @Test
    void signInRequest_shouldBeInvalidWhenPasswordIsBlank() {
        this.assertInvalid(new SignInRequest(EMAIL, ""));
    }

    @Test
    void refreshSessionRequest_shouldBeInvalidWhenRefreshTokenIsBlank() {
        this.assertInvalid(new RefreshSessionRequest("", HTTP_CONTEXT));
    }

    @Test
    void requestPasswordResetRequest_shouldBeInvalidWhenEmailIsBlankOrInvalid() {
        this.assertInvalid(new RequestPasswordResetRequest("", "es"));
        this.assertInvalid(new RequestPasswordResetRequest("invalid-email", "es"));
    }

    @Test
    void resendVerificationEmailRequest_shouldBeInvalidWhenEmailIsBlankOrInvalid() {
        this.assertInvalid(new ResendVerificationEmailRequest("", "es"));
        this.assertInvalid(new ResendVerificationEmailRequest("invalid-email", "es"));
    }

    @Test
    void resetPasswordRequest_shouldBeInvalidWhenTokenOrPasswordIsBlank() {
        this.assertInvalid(new ResetPasswordRequest("", PASSWORD));
        this.assertInvalid(new ResetPasswordRequest(TOKEN, ""));
    }

    @Test
    void changePasswordRequest_shouldBeInvalidWhenRequiredFieldsAreMissing() {
        this.assertInvalid(new ChangePasswordRequest(null, "old-password", PASSWORD));
        this.assertInvalid(new ChangePasswordRequest(USER_ID, "", PASSWORD));
        this.assertInvalid(new ChangePasswordRequest(USER_ID, "old-password", ""));
    }

    @Test
    void userIdOnlyRequests_shouldBeInvalidWhenUserIdIsNull() {
        this.assertInvalid(new DeleteUserRequest(null));
        this.assertInvalid(new ListSessionsRequest(null));
        this.assertInvalid(new RevokeAllSessionsRequest(null));
    }

    @Test
    void revokeSessionRequest_shouldBeInvalidWhenIdsAreNull() {
        this.assertInvalid(new RevokeSessionRequest(null, SESSION_ID));
        this.assertInvalid(new RevokeSessionRequest(USER_ID, null));
    }

    @Test
    void verificationEmailRequest_shouldBeInvalidWhenIdOrEmailIsMissing() {
        this.assertInvalid(new VerificationEmailRequest(null, EMAIL, "es"));
        this.assertInvalid(new VerificationEmailRequest(USER_ID, "", "es"));
    }

    @Test
    void verifyEmailRequest_shouldBeInvalidWhenRequiredFieldsAreMissing() {
        this.assertInvalid(new VerifyEmailRequest(null, TOKEN, PASSWORD));
        this.assertInvalid(new VerifyEmailRequest(USER_ID, "", PASSWORD));
        this.assertInvalid(new VerifyEmailRequest(USER_ID, TOKEN, ""));
    }

    @Test
    void verifyPasswordOtpRequest_shouldBeInvalidWhenRequiredFieldsAreMissing() {
        this.assertInvalid(new VerifyPasswordOtpRequest(null, "123456"));
        this.assertInvalid(new VerifyPasswordOtpRequest("", "123456"));
        this.assertInvalid(new VerifyPasswordOtpRequest(EMAIL, ""));
    }
}
