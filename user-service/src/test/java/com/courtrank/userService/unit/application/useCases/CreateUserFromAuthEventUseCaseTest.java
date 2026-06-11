package com.courtrank.userService.unit.application.useCases;

import com.courtrank.userService.application.dto.CreateUserRequest;
import com.courtrank.userService.application.events.UserProfileCreatedEvent;
import com.courtrank.userService.application.ports.UserEventPublisher;
import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditEventType;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.application.useCases.CreateUserFromAuthEventUseCase;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUserFromAuthEventUseCaseTest {
    @Mock
    UserRepository userRepository;

    @Mock
    UserAuditLogger auditLogger;

    @Mock
    UserEventPublisher eventPublisher;

    @InjectMocks
    CreateUserFromAuthEventUseCase useCase;

    @Test
    void execute_shouldCreateUserWithEmailVerifiedStateAndPublishEvent() {
        UUID userId = UUID.randomUUID();
        when(this.userRepository.findById(userId)).thenReturn(Optional.empty());
        when(this.userRepository.findByUsername("sebas")).thenReturn(Optional.empty());

        this.useCase.execute(new CreateUserRequest(userId, "Sebastian", "sebas", "sebas@test.com", true));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<UserAuditEvent> auditCaptor = ArgumentCaptor.forClass(UserAuditEvent.class);
        ArgumentCaptor<UserProfileCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserProfileCreatedEvent.class);
        verify(this.userRepository).save(userCaptor.capture());
        verify(this.auditLogger).log(auditCaptor.capture());
        verify(this.eventPublisher).publishUserProfileCreated(eventCaptor.capture());

        assertThat(userCaptor.getValue().getId()).isEqualTo(userId);
        assertThat(userCaptor.getValue().isEmailVerified()).isTrue();
        assertThat(userCaptor.getValue().getUserName()).isEqualTo("sebas");
        assertThat(auditCaptor.getValue().type()).isEqualTo(UserAuditEventType.USER_PROFILE_CREATED_FROM_AUTH_EVENT);
        assertThat(eventCaptor.getValue().id()).isEqualTo(userId);
    }

    @Test
    void execute_shouldSkipCreationWhenUserAlreadyExists() {
        UUID userId = UUID.randomUUID();
        User existingUser = User.create(userId, "Sebastian", "sebas", "sebas@test.com");
        when(this.userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        this.useCase.execute(new CreateUserRequest(userId, "Sebastian", "sebas", "sebas@test.com", false));

        ArgumentCaptor<UserAuditEvent> auditCaptor = ArgumentCaptor.forClass(UserAuditEvent.class);
        verify(this.userRepository, never()).save(any());
        verify(this.eventPublisher, never()).publishUserProfileCreated(any());
        verify(this.auditLogger).log(auditCaptor.capture());
        assertThat(auditCaptor.getValue().type())
                .isEqualTo(UserAuditEventType.USER_PROFILE_CREATION_SKIPPED_ALREADY_EXISTS);
    }

    @Test
    void execute_shouldCreateUserWithoutUsernameWhenUsernameIsTaken() {
        UUID userId = UUID.randomUUID();
        User usernameOwner = User.create(UUID.randomUUID(), "Other", "sebas", "other@test.com");
        when(this.userRepository.findById(userId)).thenReturn(Optional.empty());
        when(this.userRepository.findByUsername("sebas")).thenReturn(Optional.of(usernameOwner));

        this.useCase.execute(new CreateUserRequest(userId, "Sebastian", "sebas", "sebas@test.com", false));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<UserAuditEvent> auditCaptor = ArgumentCaptor.forClass(UserAuditEvent.class);
        verify(this.userRepository).save(userCaptor.capture());
        verify(this.auditLogger, org.mockito.Mockito.times(2)).log(auditCaptor.capture());

        assertThat(userCaptor.getValue().getUserName()).isNull();
        assertThat(auditCaptor.getAllValues().get(0).type())
                .isEqualTo(UserAuditEventType.USER_PROFILE_CREATION_FAILED_USERNAME_CONFLICT);
        assertThat(auditCaptor.getAllValues().get(1).type())
                .isEqualTo(UserAuditEventType.USER_PROFILE_CREATED_FROM_AUTH_EVENT);
    }
}
