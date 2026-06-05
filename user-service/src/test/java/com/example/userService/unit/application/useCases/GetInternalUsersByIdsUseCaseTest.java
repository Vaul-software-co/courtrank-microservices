package com.example.userService.unit.application.useCases;

import com.example.userService.application.dto.GetInternalUsersByIdsRequest;
import com.example.userService.application.useCases.GetInternalUsersByIdsUseCase;
import com.example.userService.domain.entity.User;
import com.example.userService.domain.repository.UserRepository;
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
class GetInternalUsersByIdsUseCaseTest {
    @Mock
    UserRepository userRepository;

    @InjectMocks
    GetInternalUsersByIdsUseCase getInternalUsersByIdsUseCase;

    @Test
    void execute_shouldReturnSummariesForExistingUsers() {
        User first = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com");
        User second = User.create(UUID.randomUUID(), "Maria", "maria", "maria@test.com");
        List<UUID> userIds = List.of(first.getId(), second.getId());

        when(this.userRepository.findByIds(userIds))
                .thenReturn(List.of(first, second));

        var results = this.getInternalUsersByIdsUseCase.execute(new GetInternalUsersByIdsRequest(userIds));

        verify(this.userRepository).findByIds(userIds);
        assertEquals(2, results.size());
        assertEquals(first.getId(), results.get(0).id());
        assertEquals(first.getEmail(), results.get(0).email());
        assertEquals(second.getId(), results.get(1).id());
    }
}
