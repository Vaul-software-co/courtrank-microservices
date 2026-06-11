package com.example.userService.unit.application.useCases;

import com.example.userService.application.ports.audit.UserAuditEvent;
import com.example.userService.application.ports.audit.UserAuditEventType;
import com.example.userService.application.ports.audit.UserAuditLogger;
import com.example.userService.application.useCases.MarkUserEmailVerifiedFromAuthEventUseCase;
import com.example.userService.domain.entity.User;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkUserEmailVerifiedFromAuthEventUseCaseTest {
    @Mock
    UserRepository userRepository;

    @Mock
    UserAuditLogger auditLogger;

    @InjectMocks
    MarkUserEmailVerifiedFromAuthEventUseCase markUserEmailVerifiedFromAuthEventUseCase;

    @Test
    void execute_shouldMarkUserEmailVerifiedAndAudit() {
        User user = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com");
        when(this.userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        this.markUserEmailVerifiedFromAuthEventUseCase.execute(user.getId());

        ArgumentCaptor<UserAuditEvent> auditCaptor = ArgumentCaptor.forClass(UserAuditEvent.class);
        verify(this.userRepository).save(user);
        verify(this.auditLogger).log(auditCaptor.capture());

        assertTrue(user.isEmailVerified());
        assertEquals(UserAuditEventType.USER_PROFILE_UPDATED, auditCaptor.getValue().type());
        assertEquals(true, auditCaptor.getValue().metadata().get("emailVerified"));
    }

    @Test
    void execute_shouldIgnoreAlreadyVerifiedUser() {
        User user = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com", true);
        when(this.userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        this.markUserEmailVerifiedFromAuthEventUseCase.execute(user.getId());

        verify(this.userRepository, never()).save(user);
        verify(this.auditLogger, never()).log(org.mockito.ArgumentMatchers.any());
    }
}
