package com.courtrank.socialService.infrastructure.controllers;

import com.courtrank.socialService.application.dto.AreUsersBlockedRequest;
import com.courtrank.socialService.application.dto.GetRelatedBlockedUserIdsRequest;
import com.courtrank.socialService.application.dto.RebuildSocialCounterRequest;
import com.courtrank.socialService.application.dto.ReconcileSocialUserRequest;
import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.application.useCases.AreUsersBlockedUseCase;
import com.courtrank.socialService.application.useCases.GetRelatedBlockedUserIdsUseCase;
import com.courtrank.socialService.application.useCases.RebuildAllSocialCountersUseCase;
import com.courtrank.socialService.application.useCases.RebuildSocialCounterUseCase;
import com.courtrank.socialService.application.useCases.ReconcileSocialUserUseCase;
import com.courtrank.socialService.domain.entity.SocialCounter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/internal/social")
@Validated
public class InternalSocialController {
    private static final String REQUEST_ID_HEADER = "x-request-id";

    private final AreUsersBlockedUseCase areUsersBlockedUseCase;
    private final GetRelatedBlockedUserIdsUseCase getRelatedBlockedUserIdsUseCase;
    private final RebuildSocialCounterUseCase rebuildSocialCounterUseCase;
    private final RebuildAllSocialCountersUseCase rebuildAllSocialCountersUseCase;
    private final ReconcileSocialUserUseCase reconcileSocialUserUseCase;

    public InternalSocialController(
            AreUsersBlockedUseCase areUsersBlockedUseCase,
            GetRelatedBlockedUserIdsUseCase getRelatedBlockedUserIdsUseCase,
            RebuildSocialCounterUseCase rebuildSocialCounterUseCase,
            RebuildAllSocialCountersUseCase rebuildAllSocialCountersUseCase,
            ReconcileSocialUserUseCase reconcileSocialUserUseCase
    ) {
        this.areUsersBlockedUseCase = areUsersBlockedUseCase;
        this.getRelatedBlockedUserIdsUseCase = getRelatedBlockedUserIdsUseCase;
        this.rebuildSocialCounterUseCase = rebuildSocialCounterUseCase;
        this.rebuildAllSocialCountersUseCase = rebuildAllSocialCountersUseCase;
        this.reconcileSocialUserUseCase = reconcileSocialUserUseCase;
    }

    @GetMapping("/blocked")
    public BlockedResponse blocked(@RequestParam UUID userA, @RequestParam UUID userB, @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId) {
        boolean blocked = this.areUsersBlockedUseCase.execute(new AreUsersBlockedRequest(userA, userB), TraceContext.fromRequestId(requestId));
        return new BlockedResponse(blocked);
    }

    @GetMapping("/users/{userId}/related-blocked-users")
    public Set<UUID> relatedBlockedUsers(@PathVariable UUID userId, @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId) {
        return this.getRelatedBlockedUserIdsUseCase.execute(new GetRelatedBlockedUserIdsRequest(userId), TraceContext.fromRequestId(requestId));
    }

    @PostMapping("/users/{userId}/reconcile")
    public void reconcile(@PathVariable UUID userId) {
        this.reconcileSocialUserUseCase.execute(new ReconcileSocialUserRequest(userId));
    }

    @PostMapping("/users/{userId}/rebuild-counter")
    public SocialCounter rebuildCounter(@PathVariable UUID userId) {
        return this.rebuildSocialCounterUseCase.execute(new RebuildSocialCounterRequest(userId));
    }

    @PostMapping("/counters/rebuild")
    public List<SocialCounter> rebuildAllCounters() {
        return this.rebuildAllSocialCountersUseCase.execute();
    }

    public record BlockedResponse(boolean blocked) {
    }

    public record ReconcileBody(@NotNull UUID userId) {
    }
}
