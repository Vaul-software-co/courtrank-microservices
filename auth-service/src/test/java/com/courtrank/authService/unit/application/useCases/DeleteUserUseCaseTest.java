package com.courtrank.authService.unit.application.useCases;

import com.courtrank.authService.application.dto.DeleteUserRequest;
import com.courtrank.authService.application.events.UserDeletedEvent;
import com.courtrank.authService.application.ports.AuthEventPublisher;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.useCases.DeleteUserUseCase;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.enums.UserRole;
import com.courtrank.authService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.domain.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteUserUseCaseTest {
    @Mock
    AuthenticationRepository authenticationRepository;

    @Mock
    SessionRepository sessionRepository;

    @Mock
    AuthEventPublisher eventPublisher;

    @Mock
    AuditLogger auditLogger;

    @InjectMocks
    DeleteUserUseCase deleteUserUseCase;

    private static final String EMAIL = "test@test.com";
    private static final String PASSWORD_HASH = "hash";
    private static final UserRole ROLE = UserRole.MEMBER;

    private Authentication createAuth() {
        return Authentication.create(
                EMAIL,
                PASSWORD_HASH,
                ROLE
        );
    }

    @Test
    void execute_shouldSoftDeleteUserAndRevokeAllSessions() {
        Authentication auth = this.createAuth();

        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));

        this.deleteUserUseCase.execute(new DeleteUserRequest(auth.getId()));

        assertTrue(auth.isDeleted());
        assertFalse(auth.isActive());
        assertNotNull(auth.getDeletedAt());
        verify(this.authenticationRepository).save(auth);
        verify(this.sessionRepository).revokeAllByUserId(auth.getId());

        ArgumentCaptor<UserDeletedEvent> eventCaptor = ArgumentCaptor.forClass(UserDeletedEvent.class);
        verify(this.eventPublisher).publishUserDeleted(eventCaptor.capture());

        UserDeletedEvent event = eventCaptor.getValue();
        assertTrue(event.id().equals(auth.getId()));
        assertTrue(event.email().equals(auth.getEmail()));
        assertNotNull(event.occurredAt());
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();

        when(this.authenticationRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.deleteUserUseCase.execute(new DeleteUserRequest(userId))
        );

        verify(this.authenticationRepository).findById(userId);
        verify(this.authenticationRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(this.sessionRepository);
        verifyNoInteractions(this.eventPublisher);
    }

    @Test
    void execute_shouldDoNothingWhenUserIsAlreadyDeleted() {
        Authentication auth = this.createAuth();
        auth.deleteUser();

        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));

        this.deleteUserUseCase.execute(new DeleteUserRequest(auth.getId()));

        assertTrue(auth.isDeleted());
        verify(this.authenticationRepository).findById(auth.getId());
        verify(this.authenticationRepository, never()).save(auth);
        verifyNoInteractions(this.sessionRepository);
        verifyNoInteractions(this.eventPublisher);
    }
}
