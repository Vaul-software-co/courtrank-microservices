package com.courtrank.socialService.infrastructure.controllers;

import com.courtrank.socialService.application.dto.AcceptFollowRequestRequest;
import com.courtrank.socialService.application.dto.BlockUserRequest;
import com.courtrank.socialService.application.dto.BlockUserResponse;
import com.courtrank.socialService.application.dto.BlockedUserSummary;
import com.courtrank.socialService.application.dto.FollowRequestSummary;
import com.courtrank.socialService.application.dto.FollowUserRequest;
import com.courtrank.socialService.application.dto.FollowUserResponse;
import com.courtrank.socialService.application.dto.ListBlockedUsersRequest;
import com.courtrank.socialService.application.dto.ListFollowRequestsRequest;
import com.courtrank.socialService.application.dto.ListFollowersRequest;
import com.courtrank.socialService.application.dto.ListFollowingRequest;
import com.courtrank.socialService.application.dto.RejectFollowRequestRequest;
import com.courtrank.socialService.application.dto.RemoveFollowerRequest;
import com.courtrank.socialService.application.dto.SearchSocialUsersRequest;
import com.courtrank.socialService.application.dto.SocialUserSummary;
import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.application.dto.UnblockUserRequest;
import com.courtrank.socialService.application.dto.UnfollowUserRequest;
import com.courtrank.socialService.application.useCases.AcceptFollowRequestUseCase;
import com.courtrank.socialService.application.useCases.BlockUserUseCase;
import com.courtrank.socialService.application.useCases.FollowUserUseCase;
import com.courtrank.socialService.application.useCases.ListBlockedUsersUseCase;
import com.courtrank.socialService.application.useCases.ListFollowersUseCase;
import com.courtrank.socialService.application.useCases.ListFollowingUseCase;
import com.courtrank.socialService.application.useCases.ListMyFollowRequestsUseCase;
import com.courtrank.socialService.application.useCases.RejectFollowRequestUseCase;
import com.courtrank.socialService.application.useCases.RemoveFollowerUseCase;
import com.courtrank.socialService.application.useCases.SearchSocialUsersUseCase;
import com.courtrank.socialService.application.useCases.UnblockUserUseCase;
import com.courtrank.socialService.application.useCases.UnfollowUserUseCase;
import com.courtrank.socialService.infrastructure.security.AuthUserPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/user")
@Validated
public class LegacyUserSocialController {
    private static final String REQUEST_ID_HEADER = "x-request-id";

    private final FollowUserUseCase followUserUseCase;
    private final AcceptFollowRequestUseCase acceptFollowRequestUseCase;
    private final RejectFollowRequestUseCase rejectFollowRequestUseCase;
    private final UnfollowUserUseCase unfollowUserUseCase;
    private final RemoveFollowerUseCase removeFollowerUseCase;
    private final BlockUserUseCase blockUserUseCase;
    private final UnblockUserUseCase unblockUserUseCase;
    private final ListFollowersUseCase listFollowersUseCase;
    private final ListFollowingUseCase listFollowingUseCase;
    private final ListMyFollowRequestsUseCase listMyFollowRequestsUseCase;
    private final ListBlockedUsersUseCase listBlockedUsersUseCase;
    private final SearchSocialUsersUseCase searchSocialUsersUseCase;

    public LegacyUserSocialController(
            FollowUserUseCase followUserUseCase,
            AcceptFollowRequestUseCase acceptFollowRequestUseCase,
            RejectFollowRequestUseCase rejectFollowRequestUseCase,
            UnfollowUserUseCase unfollowUserUseCase,
            RemoveFollowerUseCase removeFollowerUseCase,
            BlockUserUseCase blockUserUseCase,
            UnblockUserUseCase unblockUserUseCase,
            ListFollowersUseCase listFollowersUseCase,
            ListFollowingUseCase listFollowingUseCase,
            ListMyFollowRequestsUseCase listMyFollowRequestsUseCase,
            ListBlockedUsersUseCase listBlockedUsersUseCase,
            SearchSocialUsersUseCase searchSocialUsersUseCase
    ) {
        this.followUserUseCase = followUserUseCase;
        this.acceptFollowRequestUseCase = acceptFollowRequestUseCase;
        this.rejectFollowRequestUseCase = rejectFollowRequestUseCase;
        this.unfollowUserUseCase = unfollowUserUseCase;
        this.removeFollowerUseCase = removeFollowerUseCase;
        this.blockUserUseCase = blockUserUseCase;
        this.unblockUserUseCase = unblockUserUseCase;
        this.listFollowersUseCase = listFollowersUseCase;
        this.listFollowingUseCase = listFollowingUseCase;
        this.listMyFollowRequestsUseCase = listMyFollowRequestsUseCase;
        this.listBlockedUsersUseCase = listBlockedUsersUseCase;
        this.searchSocialUsersUseCase = searchSocialUsersUseCase;
    }

    @GetMapping("/search")
    public List<SocialUserSummary> search(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestParam @Size(min = 2, max = 100) String q,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        return this.searchSocialUsersUseCase.execute(new SearchSocialUsersRequest(principal.userId(), q, limit), TraceContext.fromRequestId(requestId));
    }

    @GetMapping("/me/blocked")
    public List<BlockedUserSummary> myBlockedUsers(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        return this.listBlockedUsersUseCase.execute(new ListBlockedUsersRequest(principal.userId()), TraceContext.fromRequestId(requestId));
    }

    @GetMapping("/me/follow-requests")
    public List<FollowRequestSummary> myFollowRequests(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        return this.listMyFollowRequestsUseCase.execute(new ListFollowRequestsRequest(principal.userId()), TraceContext.fromRequestId(requestId));
    }

    @PostMapping("/me/follow-requests/{followId}/accept")
    public void acceptFollowRequest(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable UUID followId,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        this.acceptFollowRequestUseCase.execute(new AcceptFollowRequestRequest(principal.userId(), followId), TraceContext.fromRequestId(requestId));
    }

    @PostMapping("/me/follow-requests/{followId}/reject")
    public void rejectFollowRequest(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable UUID followId,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        this.rejectFollowRequestUseCase.execute(new RejectFollowRequestRequest(principal.userId(), followId), TraceContext.fromRequestId(requestId));
    }

    @DeleteMapping("/me/followers/{followerId}")
    public void removeFollower(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable UUID followerId,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        this.removeFollowerUseCase.execute(new RemoveFollowerRequest(principal.userId(), followerId), TraceContext.fromRequestId(requestId));
    }

    @PostMapping("/{targetId}/block")
    public BlockUserResponse block(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable UUID targetId,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        return this.blockUserUseCase.execute(new BlockUserRequest(principal.userId(), targetId), TraceContext.fromRequestId(requestId));
    }

    @DeleteMapping("/{targetId}/block")
    public void unblock(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable UUID targetId,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        this.unblockUserUseCase.execute(new UnblockUserRequest(principal.userId(), targetId), TraceContext.fromRequestId(requestId));
    }

    @PostMapping("/{targetId}/follow")
    public FollowUserResponse follow(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable UUID targetId,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        return this.followUserUseCase.execute(new FollowUserRequest(principal.userId(), targetId), TraceContext.fromRequestId(requestId));
    }

    @DeleteMapping("/{targetId}/follow")
    public void unfollow(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable UUID targetId,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        this.unfollowUserUseCase.execute(new UnfollowUserRequest(principal.userId(), targetId), TraceContext.fromRequestId(requestId));
    }

    @GetMapping("/{userId}/followers")
    public List<SocialUserSummary> followers(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable UUID userId,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        return this.listFollowersUseCase.execute(new ListFollowersRequest(principal.userId(), userId), TraceContext.fromRequestId(requestId));
    }

    @GetMapping("/{userId}/following")
    public List<SocialUserSummary> following(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable UUID userId,
            @RequestHeader(name = REQUEST_ID_HEADER, required = false) String requestId
    ) {
        return this.listFollowingUseCase.execute(new ListFollowingRequest(principal.userId(), userId), TraceContext.fromRequestId(requestId));
    }
}
