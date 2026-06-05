package com.example.userService.application.useCases;

import com.example.userService.application.dto.SearchUsersRequest;
import com.example.userService.application.dto.UserSearchResult;
import com.example.userService.domain.entity.User;
import com.example.userService.domain.repository.UserRepository;

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
