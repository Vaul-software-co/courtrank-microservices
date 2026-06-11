package com.courtrank.userService.unit.application.useCases;

import com.courtrank.userService.application.dto.TraceContext;
import com.courtrank.userService.application.dto.UnbanUserProfileRequest;
import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditEventType;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.application.useCases.UnbanUserProfileUseCase;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.enums.UserProfileStatus;
import com.courtrank.userService.domain.exceptions.UserProfileNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnbanUserProfileUseCaseTest {
    @Mock
    UserRepository userRepository;

    @Mock
    UserAuditLogger auditLogger;

    @InjectMocks
    UnbanUserProfileUseCase useCase;

    @Test
    void execute_shouldShowSuspendedUserAndAudit() {
        UUID adminId = UUID.randomUUID();
        User user = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com");
        user.suspendProfile();
        when(this.userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        var response = this.useCase.execute(
                new UnbanUserProfileRequest(adminId, user.getId()),
                TraceContext.fromRequestId("trace-1")
        );

        ArgumentCaptor<UserAuditEvent> auditCaptor = ArgumentCaptor.forClass(UserAuditEvent.class);
        verify(this.userRepository).save(user);
        verify(this.auditLogger).log(auditCaptor.capture());
        assertThat(response.status()).isEqualTo(UserProfileStatus.VISIBLE);
        assertThat(auditCaptor.getValue().type()).isEqualTo(UserAuditEventType.USER_PROFILE_UNBANNED);
        assertThat(auditCaptor.getValue().traceId()).isEqualTo("trace-1");
    }

    @Test
    void execute_shouldReturnCurrentStatusWithoutSavingWhenUserIsNotSuspended() {
        UUID adminId = UUID.randomUUID();
        User user = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com");
        when(this.userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        var response = this.useCase.execute(
                new UnbanUserProfileRequest(adminId, user.getId()),
                TraceContext.fromRequestId("trace-2")
        );

        verify(this.userRepository, never()).save(any());
        verify(this.auditLogger, never()).log(any());
        assertThat(response.status()).isEqualTo(UserProfileStatus.VISIBLE);
    }

    @Test
    void execute_shouldAuditAndThrowWhenUserDoesNotExist() {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        when(this.userRepository.findById(targetUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.useCase.execute(
                new UnbanUserProfileRequest(adminId, targetUserId),
                TraceContext.fromRequestId("trace-3")
        )).isInstanceOf(UserProfileNotFoundException.class);

        ArgumentCaptor<UserAuditEvent> auditCaptor = ArgumentCaptor.forClass(UserAuditEvent.class);
        verify(this.auditLogger).log(auditCaptor.capture());
        assertThat(auditCaptor.getValue().type()).isEqualTo(UserAuditEventType.USER_PROFILE_UNBAN_FAILED_NOT_FOUND);
    }
}
