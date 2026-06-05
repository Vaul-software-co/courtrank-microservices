package com.example.userService.unit.application.useCases;

import com.example.userService.application.dto.AssertUserActiveRequest;
import com.example.userService.application.useCases.AssertUserActiveUseCase;
import com.example.userService.domain.entity.User;
import com.example.userService.domain.enums.UserProfileStatus;
import com.example.userService.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssertUserActiveUseCaseTest {
    @Mock
    UserRepository userRepository;

    @InjectMocks
    AssertUserActiveUseCase assertUserActiveUseCase;

    @Test
    void execute_shouldReturnFalseForSuspendedUsers() {
        User user = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com");
        user.suspendProfile();
        when(this.userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        var response = this.assertUserActiveUseCase.execute(new AssertUserActiveRequest(user.getId()));

        assertFalse(response.active());
        assertEquals(UserProfileStatus.SUSPENDED, response.status());
    }

    @Test
    void execute_shouldReturnFalseWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(this.userRepository.findById(userId))
                .thenReturn(Optional.empty());

        var response = this.assertUserActiveUseCase.execute(new AssertUserActiveRequest(userId));

        assertFalse(response.active());
        assertEquals(null, response.status());
    }
}
