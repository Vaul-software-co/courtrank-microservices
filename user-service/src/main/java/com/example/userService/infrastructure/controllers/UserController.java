package com.example.userService.infrastructure.controllers;

import com.example.userService.application.dto.GetProfileRequest;
import com.example.userService.application.dto.GetPublicProfileRequest;
import com.example.userService.application.dto.MyProfileResponse;
import com.example.userService.application.dto.PublicProfileResponse;
import com.example.userService.application.dto.RemoveMyAvatarRequest;
import com.example.userService.application.dto.RemoveMyAvatarResponse;
import com.example.userService.application.dto.SearchUsersRequest;
import com.example.userService.application.dto.TraceContext;
import com.example.userService.application.dto.UpdateMyAvatarRequest;
import com.example.userService.application.dto.UpdateMyAvatarResponse;
import com.example.userService.application.dto.UpdateMyLangRequest;
import com.example.userService.application.dto.UpdateMyLangResponse;
import com.example.userService.application.dto.UpdateMyPrivacyRequest;
import com.example.userService.application.dto.UpdateMyPrivacyResponse;
import com.example.userService.application.dto.UpdateMyProfileRequest;
import com.example.userService.application.dto.UserSearchResult;
import com.example.userService.application.useCases.GetMyProfileUseCase;
import com.example.userService.application.useCases.GetUserPublicProfileUseCase;
import com.example.userService.application.useCases.RemoveMyAvatarUseCase;
import com.example.userService.application.useCases.SearchUsersUseCase;
import com.example.userService.application.useCases.UpdateMyAvatarUseCase;
import com.example.userService.application.useCases.UpdateMyLangUseCase;
import com.example.userService.application.useCases.UpdateMyPrivacyUseCase;
import com.example.userService.application.useCases.UpdateMyProfileUseCase;
import com.example.userService.domain.enums.UserGender;
import com.example.userService.infrastructure.security.AuthUserPrincipal;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@Validated
public class UserController {
    private static final String REQUEST_ID_HEADER = "x-request-id";

    private final GetMyProfileUseCase getMyProfileUseCase;
    private final UpdateMyProfileUseCase updateMyProfileUseCase;
    private final UpdateMyPrivacyUseCase updateMyPrivacyUseCase;
    private final UpdateMyLangUseCase updateMyLangUseCase;
    private final GetUserPublicProfileUseCase getUserPublicProfileUseCase;
    private final SearchUsersUseCase searchUsersUseCase;
    private final UpdateMyAvatarUseCase updateMyAvatarUseCase;
    private final RemoveMyAvatarUseCase removeMyAvatarUseCase;

    public UserController(
            GetMyProfileUseCase getMyProfileUseCase,
            UpdateMyProfileUseCase updateMyProfileUseCase,
            UpdateMyPrivacyUseCase updateMyPrivacyUseCase,
            UpdateMyLangUseCase updateMyLangUseCase,
            GetUserPublicProfileUseCase getUserPublicProfileUseCase,
            SearchUsersUseCase searchUsersUseCase,
            UpdateMyAvatarUseCase updateMyAvatarUseCase,
            RemoveMyAvatarUseCase removeMyAvatarUseCase
    ) {
        this.getMyProfileUseCase = getMyProfileUseCase;
        this.updateMyProfileUseCase = updateMyProfileUseCase;
        this.updateMyPrivacyUseCase = updateMyPrivacyUseCase;
        this.updateMyLangUseCase = updateMyLangUseCase;
        this.getUserPublicProfileUseCase = getUserPublicProfileUseCase;
        this.searchUsersUseCase = searchUsersUseCase;
        this.updateMyAvatarUseCase = updateMyAvatarUseCase;
        this.removeMyAvatarUseCase = removeMyAvatarUseCase;
    }

    @GetMapping("/me")
    public MyProfileResponse me(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        return this.getMyProfileUseCase.execute(
                new GetProfileRequest(principal.userId()),
                TraceContext.fromRequestId(requestId)
        );
    }

    @PatchMapping("/me")
    public MyProfileResponse updateMe(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody UpdateProfileBody body,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        return this.updateMyProfileUseCase.execute(
                new UpdateMyProfileRequest(
                        principal.userId(),
                        body.name(),
                        body.username(),
                        body.phoneNumber(),
                        body.gender()
                ),
                TraceContext.fromRequestId(requestId)
        );
    }

    @PatchMapping("/me/privacy")
    public UpdateMyPrivacyResponse updatePrivacy(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody UpdatePrivacyBody body,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        return this.updateMyPrivacyUseCase.execute(
                new UpdateMyPrivacyRequest(principal.userId(), body.privateProfile()),
                TraceContext.fromRequestId(requestId)
        );
    }

    @PatchMapping("/me/lang")
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

    @GetMapping("/search")
    public List<UserSearchResult> search(
            @RequestParam
            @Size(min = 2, max = 100)
            String q,
            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(50)
            int limit
    ) {
        return this.searchUsersUseCase.execute(new SearchUsersRequest(q, limit, List.of()));
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

            UserGender gender
    ) {
    }

    public record UpdatePrivacyBody(
            @NotNull
            Boolean privateProfile
    ) {
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
}
