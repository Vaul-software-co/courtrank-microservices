package com.example.userService.unit.application.useCases;

import com.example.userService.application.dto.TraceContext;
import com.example.userService.application.dto.UpdateMyAvatarRequest;
import com.example.userService.application.ports.audit.UserAuditEvent;
import com.example.userService.application.ports.audit.UserAuditEventType;
import com.example.userService.application.ports.audit.UserAuditLogger;
import com.example.userService.application.useCases.UpdateMyAvatarUseCase;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateMyAvatarUseCaseTest {
    @Mock
    UserRepository userRepository;

    @Mock
    UserAuditLogger auditLogger;

    @InjectMocks
    UpdateMyAvatarUseCase updateMyAvatarUseCase;

    @Test
    void execute_shouldSaveAndAuditWhenAvatarChanges() {
        User user = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com");
        String avatarKey = "users/%s/avatar.webp".formatted(user.getId());
        when(this.userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        var response = this.updateMyAvatarUseCase.execute(
                new UpdateMyAvatarRequest(user.getId(), avatarKey),
                TraceContext.fromRequestId("trace-9")
        );

        ArgumentCaptor<UserAuditEvent> auditCaptor = ArgumentCaptor.forClass(UserAuditEvent.class);
        verify(this.userRepository).save(user);
        verify(this.auditLogger).log(auditCaptor.capture());

        assertEquals(avatarKey, response.avatarKey());
        assertEquals(avatarKey, response.avatarUrl());
        assertEquals(UserAuditEventType.USER_PROFILE_AVATAR_UPDATED, auditCaptor.getValue().type());
    }
}
