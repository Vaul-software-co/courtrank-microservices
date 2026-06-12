package com.courtrank.userService.unit.application.dto;

import com.courtrank.userService.application.dto.AssertUserActiveRequest;
import com.courtrank.userService.application.dto.BanUserProfileRequest;
import com.courtrank.userService.application.dto.CreateUserRequest;
import com.courtrank.userService.application.dto.GetInternalUserSummaryRequest;
import com.courtrank.userService.application.dto.GetInternalUsersByIdsRequest;
import com.courtrank.userService.application.dto.GetProfileRequest;
import com.courtrank.userService.application.dto.GetPublicProfileRequest;
import com.courtrank.userService.application.dto.RemoveMyAvatarRequest;
import com.courtrank.userService.application.dto.RestoreUserRequest;
import com.courtrank.userService.application.dto.UnbanUserProfileRequest;
import com.courtrank.userService.application.dto.UpdateMyAvatarRequest;
import com.courtrank.userService.application.dto.UpdateMyLangRequest;
import com.courtrank.userService.application.dto.UpdateMyPrivacyRequest;
import com.courtrank.userService.application.dto.UpdateMyProfileRequest;
import com.courtrank.userService.application.dto.UpdateProfileRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TARGET_USER_ID = UUID.randomUUID();

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
    void createUserRequest_shouldRequireIdNameUsernameAndEmail() {
        this.assertValid(new CreateUserRequest(USER_ID, "Sebastian", "sebas_123", "sebas@test.com", true));

        this.assertInvalid(new CreateUserRequest(null, "Sebastian", "sebas_123", "sebas@test.com", false));
        this.assertInvalid(new CreateUserRequest(USER_ID, "", "sebas_123", "sebas@test.com", false));
        this.assertInvalid(new CreateUserRequest(USER_ID, "Sebastian", "ab", "sebas@test.com", false));
        this.assertInvalid(new CreateUserRequest(USER_ID, "Sebastian", "invalid-name", "", false));
    }

    @Test
    void restoreUserRequest_shouldRequireValidUserPayload() {
        this.assertValid(new RestoreUserRequest(USER_ID, "Sebastian", "sebas_123", "sebas@test.com", true));

        this.assertInvalid(new RestoreUserRequest(null, "Sebastian", "sebas_123", "sebas@test.com", false));
        this.assertInvalid(new RestoreUserRequest(USER_ID, "", "sebas_123", "sebas@test.com", false));
        this.assertInvalid(new RestoreUserRequest(USER_ID, "Sebastian", "invalid-name", "", false));
    }

    @Test
    void idRequests_shouldRejectNullIds() {
        this.assertInvalid(new AssertUserActiveRequest(null));
        this.assertInvalid(new GetInternalUserSummaryRequest(null));
        this.assertInvalid(new GetProfileRequest(null));
        this.assertInvalid(new GetPublicProfileRequest(null));
        this.assertInvalid(new RemoveMyAvatarRequest(null));
    }

    @Test
    void adminStatusRequests_shouldRejectNullIds() {
        this.assertValid(new BanUserProfileRequest(USER_ID, TARGET_USER_ID));
        this.assertValid(new UnbanUserProfileRequest(USER_ID, TARGET_USER_ID));

        this.assertInvalid(new BanUserProfileRequest(null, TARGET_USER_ID));
        this.assertInvalid(new BanUserProfileRequest(USER_ID, null));
        this.assertInvalid(new UnbanUserProfileRequest(null, TARGET_USER_ID));
        this.assertInvalid(new UnbanUserProfileRequest(USER_ID, null));
    }

    @Test
    void getInternalUsersByIdsRequest_shouldRejectEmptyIds() {
        this.assertValid(new GetInternalUsersByIdsRequest(List.of(USER_ID)));

        this.assertInvalid(new GetInternalUsersByIdsRequest(List.of()));
    }

    @Test
    void updateMyProfileRequest_shouldValidateOptionalFieldsWhenPresent() {
        this.assertValid(new UpdateMyProfileRequest(USER_ID, "Sebastian", "sebas_123", "+573001112233", null));

        this.assertInvalid(new UpdateMyProfileRequest(null, "Sebastian", "sebas_123", "+573001112233", null));
        this.assertInvalid(new UpdateMyProfileRequest(USER_ID, "", "sebas_123", "+573001112233", null));
        this.assertInvalid(new UpdateMyProfileRequest(USER_ID, "Sebastian", "ab", "+573001112233", null));
        this.assertInvalid(new UpdateMyProfileRequest(USER_ID, "Sebastian", "invalid-name", "1234", null));
    }

    @Test
    void updateProfileRequest_shouldValidateOptionalFieldsWhenPresent() {
        this.assertValid(new UpdateProfileRequest("Sebastian", "sebas_123", "+573001112233", null));

        this.assertInvalid(new UpdateProfileRequest("", "sebas_123", "+573001112233", null));
        this.assertInvalid(new UpdateProfileRequest("Sebastian", "ab", "+573001112233", null));
        this.assertInvalid(new UpdateProfileRequest("Sebastian", "invalid-name", "1234", null));
    }

    @Test
    void avatarLangAndPrivacyRequests_shouldValidateRequiredFields() {
        this.assertValid(new UpdateMyAvatarRequest(USER_ID, "https://cdn.test/avatar.png"));
        this.assertValid(new UpdateMyLangRequest(USER_ID, "es"));
        this.assertValid(new UpdateMyPrivacyRequest(USER_ID, true));

        this.assertInvalid(new UpdateMyAvatarRequest(null, "https://cdn.test/avatar.png"));
        this.assertInvalid(new UpdateMyAvatarRequest(USER_ID, ""));
        this.assertInvalid(new UpdateMyLangRequest(null, "es"));
        this.assertInvalid(new UpdateMyLangRequest(USER_ID, "fr"));
        this.assertInvalid(new UpdateMyPrivacyRequest(USER_ID, null));
    }
}
