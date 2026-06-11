package com.courtrank.userService.unit.application.useCases;

import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditEventType;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.application.useCases.DeleteUserFromAuthEventUseCase;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteUserFromAuthEventUseCaseTest {
    @Mock
    UserRepository userRepository;

    @Mock
    UserAuditLogger auditLogger;

    @InjectMocks
    DeleteUserFromAuthEventUseCase deleteUserFromAuthEventUseCase;

    @Test
    void execute_shouldMarkProfileDeletedReleaseUsernameAndAudit() {
        User user = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com");
        when(this.userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        this.deleteUserFromAuthEventUseCase.execute(user.getId());

        ArgumentCaptor<UserAuditEvent> auditCaptor = ArgumentCaptor.forClass(UserAuditEvent.class);
        verify(this.userRepository).save(user);
        verify(this.auditLogger).log(auditCaptor.capture());

        assertEquals(UserProfileStatus.DELETED, user.getStatus());
        assertNull(user.getUserName());
        assertEquals(UserAuditEventType.USER_PROFILE_DELETED_FROM_AUTH_EVENT, auditCaptor.getValue().type());
        assertEquals("sebas", auditCaptor.getValue().metadata().get("releasedUsername"));
    }
}
