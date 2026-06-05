package com.example.userService.application.useCases;

import com.example.userService.application.dto.AssertUserActiveRequest;
import com.example.userService.application.dto.AssertUserActiveResponse;
import com.example.userService.domain.entity.User;
import com.example.userService.domain.enums.UserProfileStatus;
import com.example.userService.domain.repository.UserRepository;

public class AssertUserActiveUseCase {
    private final UserRepository userRepository;

    public AssertUserActiveUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AssertUserActiveResponse execute(AssertUserActiveRequest request) {
        User user = this.userRepository.findById(request.userId())
                .orElse(null);

        if (user == null) {
            return new AssertUserActiveResponse(false, null);
        }

        boolean active = user.getStatus() == UserProfileStatus.VISIBLE
                || user.getStatus() == UserProfileStatus.HIDDEN;

        return new AssertUserActiveResponse(active, user.getStatus());
    }
}
