package com.courtrank.userService.unit.application.useCases;

import com.courtrank.userService.application.dto.TraceContext;
import com.courtrank.userService.application.dto.UpdateMyProfileRequest;
import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditEventType;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.application.useCases.UpdateMyProfileUseCase;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.exceptions.UserNameAlreadyTakenException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateMyProfileUseCaseTest {
    @Mock
    UserRepository userRepository;

    @Mock
    UserAuditLogger auditLogger;

    @InjectMocks
    UpdateMyProfileUseCase updateMyProfileUseCase;

    @Test
    void execute_shouldPatchOnlyProvidedFieldsAndAudit() {
        User user = user("Sebastian", "sebas", "sebas@test.com");
        when(this.userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        var response = this.updateMyProfileUseCase.execute(new UpdateMyProfileRequest(
                user.getId(),
                "Sebastian Sanchez",
                null,
                "+573001112233",
                null
        ), TraceContext.fromRequestId("trace-1"));

        ArgumentCaptor<UserAuditEvent> auditCaptor = ArgumentCaptor.forClass(UserAuditEvent.class);
        verify(this.userRepository).save(user);
        verify(this.auditLogger).log(auditCaptor.capture());

        assertEquals("Sebastian Sanchez", response.name());
        assertEquals("sebas", response.username());
        assertEquals("+573001112233", response.phoneNumber());
        assertEquals(UserAuditEventType.USER_PROFILE_UPDATED, auditCaptor.getValue().type());
        assertEquals("trace-1", auditCaptor.getValue().traceId());
    }

    @Test
    void execute_shouldRejectUsernameOwnedByAnotherUser() {
        User user = user("Sebastian", "sebas", "sebas@test.com");
        User other = user("Other", "taken", "other@test.com");

        when(this.userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));
        when(this.userRepository.findByUsername("taken"))
                .thenReturn(Optional.of(other));

        assertThrows(
                UserNameAlreadyTakenException.class,
                () -> this.updateMyProfileUseCase.execute(new UpdateMyProfileRequest(
                        user.getId(),
                        null,
                        "taken",
                        null,
                        null
                ), TraceContext.fromRequestId("trace-2"))
        );

        ArgumentCaptor<UserAuditEvent> auditCaptor = ArgumentCaptor.forClass(UserAuditEvent.class);
        verify(this.userRepository, never()).save(any());
        verify(this.auditLogger).log(auditCaptor.capture());
        assertEquals(UserAuditEventType.USER_PROFILE_UPDATE_FAILED_USERNAME_CONFLICT, auditCaptor.getValue().type());
    }

    private static User user(String name, String username, String email) {
        return User.create(UUID.randomUUID(), name, username, email);
    }
}
