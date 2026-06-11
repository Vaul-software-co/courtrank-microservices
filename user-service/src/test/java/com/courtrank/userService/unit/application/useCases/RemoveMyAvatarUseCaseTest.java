package com.courtrank.userService.unit.application.useCases;

import com.courtrank.userService.application.dto.RemoveMyAvatarRequest;
import com.courtrank.userService.application.dto.TraceContext;
import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditEventType;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.application.useCases.RemoveMyAvatarUseCase;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveMyAvatarUseCaseTest {
    @Mock
    UserRepository userRepository;

    @Mock
    UserAuditLogger auditLogger;

    @InjectMocks
    RemoveMyAvatarUseCase removeMyAvatarUseCase;

    @Test
    void execute_shouldRemoveAvatarAndAuditWhenAvatarExists() {
        User user = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com");
        user.changeAvatarUrl("users/%s/avatar.webp".formatted(user.getId()));
        when(this.userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        var response = this.removeMyAvatarUseCase.execute(
                new RemoveMyAvatarRequest(user.getId()),
                TraceContext.fromRequestId("trace-10")
        );

        ArgumentCaptor<UserAuditEvent> auditCaptor = ArgumentCaptor.forClass(UserAuditEvent.class);
        verify(this.userRepository).save(user);
        verify(this.auditLogger).log(auditCaptor.capture());

        assertNull(response.avatarKey());
        assertNull(response.avatarUrl());
        assertNull(user.getAvatarUrl());
        assertEquals(UserAuditEventType.USER_PROFILE_AVATAR_REMOVED, auditCaptor.getValue().type());
    }
}
