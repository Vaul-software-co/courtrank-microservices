package com.courtrank.userService.unit.application.useCases;

import com.courtrank.userService.application.dto.SearchUsersRequest;
import com.courtrank.userService.application.useCases.SearchUsersUseCase;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchUsersUseCaseTest {
    @Mock
    UserRepository userRepository;

    @InjectMocks
    SearchUsersUseCase searchUsersUseCase;

    @Test
    void execute_shouldDelegateToRepositoryAndMapResults() {
        User user = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com");
        List<UUID> excludeIds = List.of(UUID.randomUUID());
        when(this.userRepository.searchPublic("seb", 20, excludeIds))
                .thenReturn(List.of(user));

        var results = this.searchUsersUseCase.execute(new SearchUsersRequest("seb", 20, excludeIds));

        verify(this.userRepository).searchPublic("seb", 20, excludeIds);
        assertEquals(1, results.size());
        assertEquals(user.getId(), results.get(0).id());
        assertEquals(user.getName(), results.get(0).name());
        assertEquals(user.getUserName(), results.get(0).username());
    }
}
