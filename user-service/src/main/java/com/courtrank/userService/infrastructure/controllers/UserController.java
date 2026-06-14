package com.courtrank.userService.infrastructure.controllers;

import com.courtrank.userService.application.dto.GetProfileRequest;
import com.courtrank.userService.application.dto.GetPublicProfileRequest;
import com.courtrank.userService.application.dto.ListAdminUsersRequest;
import com.courtrank.userService.application.dto.ListAdminUsersResponse;
import com.courtrank.userService.application.dto.MyProfileResponse;
import com.courtrank.userService.application.dto.PublicProfileResponse;
import com.courtrank.userService.application.dto.RemoveMyAvatarRequest;
import com.courtrank.userService.application.dto.RemoveMyAvatarResponse;
import com.courtrank.userService.application.dto.TraceContext;
import com.courtrank.userService.application.dto.UpdateMyAvatarRequest;
import com.courtrank.userService.application.dto.UpdateMyAvatarResponse;
import com.courtrank.userService.application.dto.UpdateMyLangRequest;
import com.courtrank.userService.application.dto.UpdateMyLangResponse;
import com.courtrank.userService.application.dto.UpdateMyPrivacyRequest;
import com.courtrank.userService.application.dto.UpdateMyPrivacyResponse;
import com.courtrank.userService.application.dto.UpdateMyProfileRequest;
import com.courtrank.userService.application.useCases.GetMyProfileUseCase;
import com.courtrank.userService.application.useCases.GetUserPublicProfileUseCase;
import com.courtrank.userService.application.useCases.ListAdminUsersUseCase;
import com.courtrank.userService.application.useCases.RemoveMyAvatarUseCase;
import com.courtrank.userService.application.useCases.UpdateMyAvatarUseCase;
import com.courtrank.userService.application.useCases.UpdateMyLangUseCase;
import com.courtrank.userService.application.useCases.UpdateMyPrivacyUseCase;
import com.courtrank.userService.application.useCases.UpdateMyProfileUseCase;
import com.courtrank.userService.domain.enums.UserGender;
import com.courtrank.userService.infrastructure.security.AuthUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping({"/users", "/user"})
@Validated
public class UserController {
    private static final String REQUEST_ID_HEADER = "x-request-id";

    private final GetMyProfileUseCase getMyProfileUseCase;
    private final UpdateMyProfileUseCase updateMyProfileUseCase;
    private final UpdateMyPrivacyUseCase updateMyPrivacyUseCase;
    private final UpdateMyLangUseCase updateMyLangUseCase;
    private final GetUserPublicProfileUseCase getUserPublicProfileUseCase;
    private final ListAdminUsersUseCase listAdminUsersUseCase;
    private final UpdateMyAvatarUseCase updateMyAvatarUseCase;
    private final RemoveMyAvatarUseCase removeMyAvatarUseCase;

    public UserController(
            GetMyProfileUseCase getMyProfileUseCase,
            UpdateMyProfileUseCase updateMyProfileUseCase,
            UpdateMyPrivacyUseCase updateMyPrivacyUseCase,
            UpdateMyLangUseCase updateMyLangUseCase,
            GetUserPublicProfileUseCase getUserPublicProfileUseCase,
            ListAdminUsersUseCase listAdminUsersUseCase,
            UpdateMyAvatarUseCase updateMyAvatarUseCase,
            RemoveMyAvatarUseCase removeMyAvatarUseCase
    ) {
        this.getMyProfileUseCase = getMyProfileUseCase;
        this.updateMyProfileUseCase = updateMyProfileUseCase;
        this.updateMyPrivacyUseCase = updateMyPrivacyUseCase;
        this.updateMyLangUseCase = updateMyLangUseCase;
        this.getUserPublicProfileUseCase = getUserPublicProfileUseCase;
        this.listAdminUsersUseCase = listAdminUsersUseCase;
        this.updateMyAvatarUseCase = updateMyAvatarUseCase;
        this.removeMyAvatarUseCase = removeMyAvatarUseCase;
    }

    @GetMapping("/me")
    public Object me(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId,
            HttpServletRequest servletRequest
    ) {
        MyProfileResponse response = this.getMyProfileUseCase.execute(
                new GetProfileRequest(principal.userId()),
                TraceContext.fromRequestId(requestId)
        );
        return this.isLegacyUserPath(servletRequest) ? LegacyMyProfileResponse.from(response) : response;
    }

    @PatchMapping("/me")
    public Object updateMe(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody UpdateProfileBody body,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId,
            HttpServletRequest servletRequest
    ) {
        MyProfileResponse response = this.updateMyProfileUseCase.execute(
                new UpdateMyProfileRequest(
                        principal.userId(),
                        body.name(),
                        body.username(),
                        body.resolvedPhoneNumber(),
                        body.gender()
                ),
                TraceContext.fromRequestId(requestId)
        );
        return this.isLegacyUserPath(servletRequest) ? LegacyMyProfileResponse.from(response) : response;
    }

    @PatchMapping("/me/privacy")
    public Object updatePrivacy(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody UpdatePrivacyBody body,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId,
            HttpServletRequest servletRequest
    ) {
        UpdateMyPrivacyResponse response = this.updateMyPrivacyUseCase.execute(
                new UpdateMyPrivacyRequest(principal.userId(), body.resolvedPrivateProfile()),
                TraceContext.fromRequestId(requestId)
        );
        return this.isLegacyUserPath(servletRequest) ? new LegacyPrivacyResponse(response.privateProfile()) : response;
    }

    @RequestMapping(value = "/me/lang", method = {RequestMethod.PATCH, RequestMethod.POST})
    public UpdateMyLangResponse updateLang(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody UpdateLangBody body,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        return this.updateMyLangUseCase.execute(
                new UpdateMyLangRequest(principal.userId(), body.lang()),
                TraceContext.fromRequestId(requestId)
        );
    }

    @PutMapping("/me/avatar")
    public UpdateMyAvatarResponse updateAvatar(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody UpdateAvatarBody body,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        return this.updateMyAvatarUseCase.execute(
                new UpdateMyAvatarRequest(principal.userId(), body.avatarKey()),
                TraceContext.fromRequestId(requestId)
        );
    }

    @DeleteMapping("/me/avatar")
    public RemoveMyAvatarResponse removeAvatar(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        return this.removeMyAvatarUseCase.execute(
                new RemoveMyAvatarRequest(principal.userId()),
                TraceContext.fromRequestId(requestId)
        );
    }

    @GetMapping("/admin")
    public ListAdminUsersResponse adminUsers(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        return this.listAdminUsersUseCase.execute(new ListAdminUsersRequest(q, limit, offset));
    }

    @GetMapping("/{id}")
    public PublicProfileResponse publicProfile(
            @PathVariable UUID id,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        return this.getUserPublicProfileUseCase.execute(
                new GetPublicProfileRequest(id),
                TraceContext.fromRequestId(requestId)
        );
    }

    public record UpdateProfileBody(
            @Size(min = 1, max = 100)
            String name,

            @Size(min = 3, max = 30)
            @Pattern(regexp = "^[a-zA-ZñÑ0-9_]+$", message = "Username can only contain letters, numbers and underscores")
            String username,

            @Size(min = 5, max = 20)
            String phoneNumber,

            @Size(min = 5, max = 20)
            String phone,

            UserGender gender
    ) {
        String resolvedPhoneNumber() {
            return this.phoneNumber != null ? this.phoneNumber : this.phone;
        }
    }

    public record UpdatePrivacyBody(
            @NotNull
            Boolean privateProfile,
            Boolean isPrivate
    ) {
        public UpdatePrivacyBody {
            if (privateProfile == null) {
                privateProfile = isPrivate;
            }
        }

        Boolean resolvedPrivateProfile() {
            return this.privateProfile;
        }
    }

    public record UpdateLangBody(
            @NotBlank
            @Pattern(regexp = "^(es|en)$", message = "Language must be es or en")
            String lang
    ) {
    }

    public record UpdateAvatarBody(
            @NotBlank
            String avatarKey
    ) {
    }

    private boolean isLegacyUserPath(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/user/");
    }

    public record LegacyMyProfileResponse(
            UUID id,
            String name,
            String username,
            String email,
            String phone,
            UserGender gender,
            String avatarUrl,
            boolean isEmailVerified,
            boolean isPrivate,
            String status,
            String lang,
            Instant createdAt,
            Instant acceptedDataCommercializationAt,
            boolean needsTermsAcceptance,
            UsernameChangeInfo usernameChangeInfo
    ) {
        static LegacyMyProfileResponse from(MyProfileResponse response) {
            return new LegacyMyProfileResponse(
                    response.id(),
                    response.name(),
                    response.username(),
                    response.email(),
                    response.phoneNumber(),
                    response.gender(),
                    response.avatarUrl(),
                    response.isEmailVerified(),
                    response.privateProfile(),
                    response.status().name(),
                    response.lang(),
                    response.createdAt(),
                    null,
                    false,
                    new UsernameChangeInfo(0, 2, null)
            );
        }
    }

    public record UsernameChangeInfo(
            int changesUsed,
            int changesLeft,
            String nextAvailableAt
    ) {
    }

    public record LegacyPrivacyResponse(
            Boolean isPrivate
    ) {
    }
}
