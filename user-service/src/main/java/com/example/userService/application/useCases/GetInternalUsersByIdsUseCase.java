package com.example.userService.application.useCases;

import com.example.userService.application.dto.GetInternalUsersByIdsRequest;
import com.example.userService.application.dto.InternalUserSummaryResponse;
import com.example.userService.domain.entity.User;
import com.example.userService.domain.repository.UserRepository;

import java.util.List;

public class GetInternalUsersByIdsUseCase {
    private final UserRepository userRepository;

    public GetInternalUsersByIdsUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<InternalUserSummaryResponse> execute(GetInternalUsersByIdsRequest request) {
        return this.userRepository.findByIds(request.userIds())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private InternalUserSummaryResponse toResponse(User user) {
        return new InternalUserSummaryResponse(
                user.getId(),
                user.getName(),
                user.getUserName(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.isPrivateProfile(),
                user.getStatus()
        );
    }
}
