package com.example.userService.unit.application.useCases;

import com.example.userService.application.dto.TraceContext;
import com.example.userService.application.dto.UpdateMyPrivacyRequest;
import com.example.userService.application.ports.audit.UserAuditEvent;
import com.example.userService.application.ports.audit.UserAuditEventType;
import com.example.userService.application.ports.audit.UserAuditLogger;
import com.example.userService.application.useCases.UpdateMyPrivacyUseCase;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateMyPrivacyUseCaseTest {
    @Mock
    UserRepository userRepository;

    @Mock
    UserAuditLogger auditLogger;

    @InjectMocks
    UpdateMyPrivacyUseCase updateMyPrivacyUseCase;

    @Test
    void execute_shouldReturnWithoutSaveOrAuditWhenValueDoesNotChange() {
        User user = user();
        user.changePrivacy(true);
        when(this.userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        var response = this.updateMyPrivacyUseCase.execute(
                new UpdateMyPrivacyRequest(user.getId(), true),
                TraceContext.fromRequestId("trace-3")
        );

        assertTrue(response.privateProfile());
        verify(this.userRepository, never()).save(any());
        verifyNoInteractions(this.auditLogger);
    }

    @Test
    void execute_shouldSaveAndAuditWhenValueChanges() {
        User user = user();
        when(this.userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        var response = this.updateMyPrivacyUseCase.execute(
                new UpdateMyPrivacyRequest(user.getId(), true),
                TraceContext.fromRequestId("trace-4")
        );

        ArgumentCaptor<UserAuditEvent> auditCaptor = ArgumentCaptor.forClass(UserAuditEvent.class);
        verify(this.userRepository).save(user);
        verify(this.auditLogger).log(auditCaptor.capture());

        assertTrue(response.privateProfile());
        assertEquals(UserAuditEventType.USER_PROFILE_PRIVACY_UPDATED, auditCaptor.getValue().type());
        assertEquals(false, auditCaptor.getValue().metadata().get("previousPrivateProfile"));
        assertEquals(true, auditCaptor.getValue().metadata().get("privateProfile"));
    }

    private static User user() {
        return User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com");
    }
}
