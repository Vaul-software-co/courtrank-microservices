package com.courtrank.userService.application.useCases;

import com.courtrank.userService.application.dto.AdminUserSummaryResponse;
import com.courtrank.userService.application.dto.ListAdminUsersRequest;
import com.courtrank.userService.application.dto.ListAdminUsersResponse;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.repository.UserRepository;

public class ListAdminUsersUseCase {
    private final UserRepository userRepository;

    public ListAdminUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ListAdminUsersResponse execute(ListAdminUsersRequest request) {
        var users = this.userRepository.searchForAdmin(request.query(), request.limit(), request.offset())
                .stream()
                .map(this::toResponse)
                .toList();
        long total = this.userRepository.countForAdmin(request.query());

        return new ListAdminUsersResponse(users, request.limit(), request.offset(), total);
    }

    private AdminUserSummaryResponse toResponse(User user) {
        return new AdminUserSummaryResponse(
                user.getId(),
                user.getName(),
                user.getUserName(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.isPrivateProfile(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
