package com.courtrank.userService.application.useCases;

import com.courtrank.userService.application.dto.AssertUserActiveRequest;
import com.courtrank.userService.application.dto.AssertUserActiveResponse;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.enums.UserProfileStatus;
import com.courtrank.userService.domain.repository.UserRepository;

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
