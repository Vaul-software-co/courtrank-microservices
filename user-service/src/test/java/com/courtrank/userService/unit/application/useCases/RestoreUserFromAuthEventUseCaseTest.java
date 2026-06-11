package com.courtrank.userService.unit.application.useCases;

import com.courtrank.userService.application.dto.RestoreUserRequest;
import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditEventType;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.application.useCases.RestoreUserFromAuthEventUseCase;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.enums.UserProfileStatus;
import com.courtrank.userService.domain.repository.UserRepository;
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
                new RestoreUserRequest(user.getId(), user.getName(), user.getUserName(), user.getEmail(), false)
        );

        ArgumentCaptor<UserAuditEvent> auditCaptor = ArgumentCaptor.forClass(UserAuditEvent.class);
        verify(this.userRepository).save(user);
        verify(this.auditLogger).log(auditCaptor.capture());

        assertEquals(UserProfileStatus.VISIBLE, user.getStatus());
        assertEquals(UserAuditEventType.USER_PROFILE_RESTORED_FROM_AUTH_EVENT, auditCaptor.getValue().type());
    }
}
