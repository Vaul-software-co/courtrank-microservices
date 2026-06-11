package com.courtrank.userService.unit.application.useCases;

import com.courtrank.userService.application.dto.GetProfileRequest;
import com.courtrank.userService.application.dto.TraceContext;
import com.courtrank.userService.application.ports.audit.UserAuditEvent;
import com.courtrank.userService.application.ports.audit.UserAuditEventType;
import com.courtrank.userService.application.ports.audit.UserAuditLogger;
import com.courtrank.userService.application.useCases.GetMyProfileUseCase;
import com.courtrank.userService.domain.entity.User;
import com.courtrank.userService.domain.enums.UserGender;
import com.courtrank.userService.domain.exceptions.InvalidCredentialsException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMyProfileUseCaseTest {
    @Mock
    UserRepository userRepository;

    @Mock
    UserAuditLogger auditLogger;

    @InjectMocks
    GetMyProfileUseCase useCase;

    @Test
    void execute_shouldReturnCompleteProfile() {
        User user = User.create(UUID.randomUUID(), "Sebastian", "sebas", "sebas@test.com", true);
        user.changePhoneNumber("+573001112233");
        user.changeGender(UserGender.MALE);
        user.changeAvatarUrl("https://cdn.test/avatar.png");
        user.changeLang("es");
        user.changePrivacy(true);
        when(this.userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        var response = this.useCase.execute(new GetProfileRequest(user.getId()), TraceContext.fromRequestId("trace-1"));

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.name()).isEqualTo("Sebastian");
        assertThat(response.username()).isEqualTo("sebas");
        assertThat(response.email()).isEqualTo("sebas@test.com");
        assertThat(response.isEmailVerified()).isTrue();
        assertThat(response.phoneNumber()).isEqualTo("+573001112233");
        assertThat(response.gender()).isEqualTo(UserGender.MALE);
        assertThat(response.avatarUrl()).isEqualTo("https://cdn.test/avatar.png");
        assertThat(response.privateProfile()).isTrue();
        assertThat(response.lang()).isEqualTo("es");
    }

    @Test
    void execute_shouldAuditAndThrowWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(this.userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.useCase.execute(new GetProfileRequest(userId), TraceContext.fromRequestId("trace-2")))
                .isInstanceOf(InvalidCredentialsException.class);

        ArgumentCaptor<UserAuditEvent> auditCaptor = ArgumentCaptor.forClass(UserAuditEvent.class);
        verify(this.auditLogger).log(auditCaptor.capture());
        assertThat(auditCaptor.getValue().type()).isEqualTo(UserAuditEventType.USER_PROFILE_LOOKUP_FAILED_NOT_FOUND);
        assertThat(auditCaptor.getValue().traceId()).isEqualTo("trace-2");
    }
}
