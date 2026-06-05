package com.example.userService.unit.application.useCases;

import com.example.userService.application.dto.RestoreUserRequest;
import com.example.userService.application.ports.audit.UserAuditEvent;
import com.example.userService.application.ports.audit.UserAuditEventType;
import com.example.userService.application.ports.audit.UserAuditLogger;
import com.example.userService.application.useCases.RestoreUserFromAuthEventUseCase;
import com.example.userService.domain.entity.User;
import com.example.userService.domain.enums.UserProfileStatus;
import com.example.userService.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestoreUserFromAuthEventUseCaseTest {
    @Mock
    UserRepository userRepository;

    @Mock
    UserAuditLogger auditLogger;

    @InjectMocks
    RestoreUserFromAuthEventUseCase restoreUserFromAuthEventUseCase;

    @Test
    void execute_shouldRestoreDeletedUserAndAudit() {
        User user = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com");
        user.markProfileAsDeleted();
        when(this.userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        this.restoreUserFromAuthEventUseCase.execute(
                new RestoreUserRequest(user.getId(), user.getName(), user.getUserName(), user.getEmail())
        );

        ArgumentCaptor<UserAuditEvent> auditCaptor = ArgumentCaptor.forClass(UserAuditEvent.class);
        verify(this.userRepository).save(user);
        verify(this.auditLogger).log(auditCaptor.capture());

        assertEquals(UserProfileStatus.VISIBLE, user.getStatus());
        assertEquals(UserAuditEventType.USER_PROFILE_RESTORED_FROM_AUTH_EVENT, auditCaptor.getValue().type());
    }
}
