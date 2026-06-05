package com.example.userService.unit.application.useCases;

import com.example.userService.application.dto.TraceContext;
import com.example.userService.application.dto.UpdateMyLangRequest;
import com.example.userService.application.ports.audit.UserAuditEvent;
import com.example.userService.application.ports.audit.UserAuditEventType;
import com.example.userService.application.ports.audit.UserAuditLogger;
import com.example.userService.application.useCases.UpdateMyLangUseCase;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateMyLangUseCaseTest {
    @Mock
    UserRepository userRepository;

    @Mock
    UserAuditLogger auditLogger;

    @InjectMocks
    UpdateMyLangUseCase updateMyLangUseCase;

    @Test
    void execute_shouldReturnWithoutSaveOrAuditWhenValueDoesNotChange() {
        User user = user();
        user.changeLang("es");
        when(this.userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        var response = this.updateMyLangUseCase.execute(
                new UpdateMyLangRequest(user.getId(), "es"),
                TraceContext.fromRequestId("trace-5")
        );

        assertEquals("es", response.lang());
        verify(this.userRepository, never()).save(any());
        verifyNoInteractions(this.auditLogger);
    }

    @Test
    void execute_shouldSaveAndAuditWhenValueChanges() {
        User user = user();
        user.changeLang("es");
        when(this.userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        var response = this.updateMyLangUseCase.execute(
                new UpdateMyLangRequest(user.getId(), "en"),
                TraceContext.fromRequestId("trace-6")
        );

        ArgumentCaptor<UserAuditEvent> auditCaptor = ArgumentCaptor.forClass(UserAuditEvent.class);
        verify(this.userRepository).save(user);
        verify(this.auditLogger).log(auditCaptor.capture());

        assertEquals("en", response.lang());
        assertEquals(UserAuditEventType.USER_PROFILE_LANG_UPDATED, auditCaptor.getValue().type());
        assertEquals("es", auditCaptor.getValue().metadata().get("previousLang"));
        assertEquals("en", auditCaptor.getValue().metadata().get("lang"));
    }

    private static User user() {
        return User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com");
    }
}
