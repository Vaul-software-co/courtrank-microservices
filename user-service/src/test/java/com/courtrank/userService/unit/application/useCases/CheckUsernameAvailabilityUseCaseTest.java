package com.courtrank.userService.unit.application.useCases;

import com.courtrank.userService.application.useCases.CheckUsernameAvailabilityUseCase;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckUsernameAvailabilityUseCaseTest {
    @Mock
    UserRepository userRepository;

    @InjectMocks
    CheckUsernameAvailabilityUseCase useCase;

    @Test
    void execute_shouldReturnTrueWhenUsernameIsUnused() {
        when(this.userRepository.findByUsername("sebas")).thenReturn(Optional.empty());

        assertThat(this.useCase.execute("sebas", UUID.randomUUID())).isTrue();
    }

    @Test
    void execute_shouldReturnTrueWhenUsernameBelongsToSameUser() {
        UUID userId = UUID.randomUUID();
        when(this.userRepository.findByUsername("sebas"))
                .thenReturn(Optional.of(User.create(userId, "Sebastian", "sebas", "sebas@test.com")));

        assertThat(this.useCase.execute("sebas", userId)).isTrue();
    }

    @Test
    void execute_shouldReturnFalseWhenUsernameBelongsToAnotherUser() {
        when(this.userRepository.findByUsername("sebas"))
                .thenReturn(Optional.of(User.create(UUID.randomUUID(), "Other", "sebas", "other@test.com")));

        assertThat(this.useCase.execute("sebas", UUID.randomUUID())).isFalse();
    }
}
