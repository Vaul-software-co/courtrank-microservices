package com.example.userService.application.useCases;

import com.example.userService.application.dto.GetInternalUserSummaryRequest;
import com.example.userService.application.dto.InternalUserSummaryResponse;
import com.example.userService.domain.entity.User;
import com.example.userService.domain.exceptions.UserProfileNotFoundException;
import com.example.userService.domain.repository.UserRepository;

public class GetInternalUserSummaryUseCase {
    private final UserRepository userRepository;

    public GetInternalUserSummaryUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public InternalUserSummaryResponse execute(GetInternalUserSummaryRequest request) {
        User user = this.userRepository.findById(request.userId())
                .orElseThrow(UserProfileNotFoundException::new);

        return this.toResponse(user);
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
