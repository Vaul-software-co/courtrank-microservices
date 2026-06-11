package com.courtrank.userService.infrastructure.controllers;

import com.courtrank.userService.application.dto.AssertUserActiveRequest;
import com.courtrank.userService.application.dto.AssertUserActiveResponse;
import com.courtrank.userService.application.dto.BanUserProfileRequest;
import com.courtrank.userService.application.dto.GetInternalUserSummaryRequest;
import com.courtrank.userService.application.dto.GetInternalUsersByIdsRequest;
import com.courtrank.userService.application.dto.InternalUserSummaryResponse;
import com.courtrank.userService.application.dto.TraceContext;
import com.courtrank.userService.application.dto.UnbanUserProfileRequest;
import com.courtrank.userService.application.dto.UsernameAvailabilityResponse;
import com.courtrank.userService.application.dto.UserProfileStatusResponse;
import com.courtrank.userService.application.useCases.AssertUserActiveUseCase;
import com.courtrank.userService.application.useCases.BanUserProfileUseCase;
import com.courtrank.userService.application.useCases.CheckUsernameAvailabilityUseCase;
import com.courtrank.userService.application.useCases.GetInternalUserSummaryUseCase;
import com.courtrank.userService.application.useCases.GetInternalUsersByIdsUseCase;
import com.courtrank.userService.application.useCases.UnbanUserProfileUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/users")
@Validated
public class InternalUserController {
    private static final String REQUEST_ID_HEADER = "x-request-id";

    private final CheckUsernameAvailabilityUseCase checkUsernameAvailabilityUseCase;
    private final GetInternalUserSummaryUseCase getInternalUserSummaryUseCase;
    private final GetInternalUsersByIdsUseCase getInternalUsersByIdsUseCase;
    private final AssertUserActiveUseCase assertUserActiveUseCase;
    private final BanUserProfileUseCase banUserProfileUseCase;
    private final UnbanUserProfileUseCase unbanUserProfileUseCase;

    public InternalUserController(
            CheckUsernameAvailabilityUseCase checkUsernameAvailabilityUseCase,
            GetInternalUserSummaryUseCase getInternalUserSummaryUseCase,
            GetInternalUsersByIdsUseCase getInternalUsersByIdsUseCase,
            AssertUserActiveUseCase assertUserActiveUseCase,
            BanUserProfileUseCase banUserProfileUseCase,
            UnbanUserProfileUseCase unbanUserProfileUseCase
    ) {
        this.checkUsernameAvailabilityUseCase = checkUsernameAvailabilityUseCase;
        this.getInternalUserSummaryUseCase = getInternalUserSummaryUseCase;
        this.getInternalUsersByIdsUseCase = getInternalUsersByIdsUseCase;
        this.assertUserActiveUseCase = assertUserActiveUseCase;
        this.banUserProfileUseCase = banUserProfileUseCase;
        this.unbanUserProfileUseCase = unbanUserProfileUseCase;
    }

    @GetMapping("/username-available")
    public UsernameAvailabilityResponse usernameAvailable(
            @RequestParam
            @Size(min = 3, max = 30)
            @Pattern(regexp = "^[a-zA-ZñÑ0-9_]+$", message = "Username can only contain letters, numbers and underscores")
            String username,
            @RequestParam UUID userId
    ) {
        return new UsernameAvailabilityResponse(
                this.checkUsernameAvailabilityUseCase.execute(username, userId)
        );
    }

    @GetMapping("/{id}/summary")
    public InternalUserSummaryResponse summary(
            @PathVariable UUID id
    ) {
        return this.getInternalUserSummaryUseCase.execute(new GetInternalUserSummaryRequest(id));
    }

    @PostMapping("/summaries")
    public List<InternalUserSummaryResponse> summaries(
            @Valid @RequestBody GetInternalUsersByIdsRequest request
    ) {
        return this.getInternalUsersByIdsUseCase.execute(request);
    }

    @GetMapping("/{id}/active")
    public AssertUserActiveResponse active(
            @PathVariable UUID id
    ) {
        return this.assertUserActiveUseCase.execute(new AssertUserActiveRequest(id));
    }

    @PostMapping("/{id}/ban")
    public UserProfileStatusResponse ban(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUserActionBody body,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        return this.banUserProfileUseCase.execute(
                new BanUserProfileRequest(body.adminUserId(), id),
                TraceContext.fromRequestId(requestId)
        );
    }

    @PostMapping("/{id}/unban")
    public UserProfileStatusResponse unban(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUserActionBody body,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        return this.unbanUserProfileUseCase.execute(
                new UnbanUserProfileRequest(body.adminUserId(), id),
                TraceContext.fromRequestId(requestId)
        );
    }

    public record AdminUserActionBody(
            @NotNull
            UUID adminUserId
    ) {
    }
}
