package com.courtrank.userService.unit.application.useCases;

import com.courtrank.userService.application.dto.GetPublicProfileRequest;
import com.courtrank.userService.application.dto.TraceContext;
import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditEventType;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.application.useCases.GetUserPublicProfileUseCase;
import com.courtrank.userService.domain.entity.User;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserPublicProfileUseCaseTest {
    @Mock
    UserRepository userRepository;

    @Mock
    UserAuditLogger auditLogger;

    @InjectMocks
    GetUserPublicProfileUseCase getUserPublicProfileUseCase;

    @Test
    void execute_shouldReturnVisiblePublicProfile() {
        User user = user();
        when(this.userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        var response = this.getUserPublicProfileUseCase.execute(
                new GetPublicProfileRequest(user.getId()),
                TraceContext.fromRequestId("trace-7")
        );

        assertEquals(user.getId(), response.id());
        assertEquals(user.getName(), response.name());
        assertEquals(user.getUserName(), response.username());
    }

    @Test
    void execute_shouldHideSuspendedUsersAsNotFoundAndAudit() {
        User user = user();
        user.suspendProfile();
        when(this.userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        assertThrows(
                UserProfileNotFoundException.class,
                () -> this.getUserPublicProfileUseCase.execute(
                        new GetPublicProfileRequest(user.getId()),
                        TraceContext.fromRequestId("trace-8")
                )
        );

        ArgumentCaptor<UserAuditEvent> auditCaptor = ArgumentCaptor.forClass(UserAuditEvent.class);
        verify(this.auditLogger).log(auditCaptor.capture());
        assertEquals(UserAuditEventType.USER_PROFILE_PUBLIC_LOOKUP_FAILED_NOT_FOUND, auditCaptor.getValue().type());
    }

    private static User user() {
        return User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com");
    }
}
