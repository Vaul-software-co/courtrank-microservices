package com.example.userService.unit.application.useCases;

import com.example.userService.application.dto.BanUserProfileRequest;
import com.example.userService.application.dto.TraceContext;
import com.example.userService.application.ports.audit.UserAuditEvent;
import com.example.userService.application.ports.audit.UserAuditEventType;
import com.example.userService.application.ports.audit.UserAuditLogger;
import com.example.userService.application.useCases.BanUserProfileUseCase;
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
class BanUserProfileUseCaseTest {
    @Mock
    UserRepository userRepository;

    @Mock
    UserAuditLogger auditLogger;

    @InjectMocks
    BanUserProfileUseCase banUserProfileUseCase;

    @Test
    void execute_shouldSuspendVisibleUserAndAudit() {
        UUID adminId = UUID.randomUUID();
        User user = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com");
        when(this.userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        var response = this.banUserProfileUseCase.execute(
                new BanUserProfileRequest(adminId, user.getId()),
                TraceContext.fromRequestId("trace-11")
        );

        ArgumentCaptor<UserAuditEvent> auditCaptor = ArgumentCaptor.forClass(UserAuditEvent.class);
        verify(this.userRepository).save(user);
        verify(this.auditLogger).log(auditCaptor.capture());

        assertEquals(UserProfileStatus.SUSPENDED, response.status());
        assertEquals(UserAuditEventType.USER_PROFILE_BANNED, auditCaptor.getValue().type());
        assertEquals(adminId, auditCaptor.getValue().actorId());
    }
}
