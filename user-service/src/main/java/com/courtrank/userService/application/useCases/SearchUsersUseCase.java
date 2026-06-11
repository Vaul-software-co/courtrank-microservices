package com.courtrank.userService.application.useCases;

import com.courtrank.userService.application.dto.SearchUsersRequest;
import com.courtrank.userService.application.dto.UserSearchResult;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.repository.UserRepository;

import java.util.List;

public class SearchUsersUseCase {
    private final UserRepository userRepository;

    public SearchUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserSearchResult> execute(SearchUsersRequest request) {
        return this.userRepository.searchPublic(request.query(), request.limit(), request.excludeIds())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private UserSearchResult toResponse(User user) {
        return new UserSearchResult(
                user.getId(),
                user.getName(),
                user.getUserName(),
                user.getAvatarUrl()
        );
    }
}
