package com.courtrank.userService.unit.application.useCases;

import com.courtrank.userService.application.dto.GetInternalUserSummaryRequest;
import com.courtrank.userService.application.useCases.GetInternalUserSummaryUseCase;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.exceptions.UserProfileNotFoundException;
import com.courtrank.userService.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetInternalUserSummaryUseCaseTest {
    @Mock
    UserRepository userRepository;

    @InjectMocks
    GetInternalUserSummaryUseCase useCase;

    @Test
    void execute_shouldReturnInternalSummary() {
        User user = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com");
        user.changeAvatarUrl("https://cdn.test/avatar.png");
        user.changePrivacy(true);
        when(this.userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        var response = this.useCase.execute(new GetInternalUserSummaryRequest(user.getId()));

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.name()).isEqualTo("Sebastian");
        assertThat(response.username()).isEqualTo("sebas");
        assertThat(response.email()).isEqualTo("sebas@test.com");
        assertThat(response.avatarUrl()).isEqualTo("https://cdn.test/avatar.png");
        assertThat(response.privateProfile()).isTrue();
        assertThat(response.status()).isEqualTo(user.getStatus());
    }

    @Test
    void execute_shouldThrowWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(this.userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.useCase.execute(new GetInternalUserSummaryRequest(userId)))
                .isInstanceOf(UserProfileNotFoundException.class);
    }
}
