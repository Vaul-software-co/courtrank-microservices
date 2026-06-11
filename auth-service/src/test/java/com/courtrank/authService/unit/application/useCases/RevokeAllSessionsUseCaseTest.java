package com.courtrank.authService.unit.application.useCases;

import com.courtrank.authService.application.dto.RevokeAllSessionsRequest;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.useCases.RevokeAllSessionsUseCase;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.enums.UserRole;
import com.courtrank.authService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.domain.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RevokeAllSessionsUseCaseTest {
    @Mock
    AuthenticationRepository authenticationRepository;

    @Mock
    SessionRepository sessionRepository;

    @Mock
    AuditLogger auditLogger;

    @InjectMocks
    RevokeAllSessionsUseCase revokeAllSessionsUseCase;

    private static final String EMAIL = "test@test.com";
    private static final String PASSWORD_HASH = "hash";

    private Authentication createAuth() {
        return Authentication.create(EMAIL, PASSWORD_HASH, UserRole.MEMBER);
    }

    private Authentication createInactiveAuth() {
        Authentication auth = this.createAuth();

        return Authentication.restore(
                auth.getId(),
                auth.getEmail(),
                auth.getPasswordHash(),
                auth.getRole(),
                auth.isEmailVerified(),
                false,
                auth.getTermsVersionAccepted(),
                auth.getTermsAcceptedAt(),
                null,
                auth.getDeletedAt(),
                auth.getCreatedAt(),
                auth.getUpdatedAt()
        );
    }

    @Test
    void execute_shouldRevokeAllSessionsForActiveUser() {
        Authentication auth = this.createAuth();

        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));

        this.revokeAllSessionsUseCase.execute(new RevokeAllSessionsRequest(auth.getId()));

        verify(this.sessionRepository).revokeAllByUserId(auth.getId());
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();

        when(this.authenticationRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.revokeAllSessionsUseCase.execute(new RevokeAllSessionsRequest(userId))
        );

        verifyNoInteractions(this.sessionRepository);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserIsDeleted() {
        Authentication auth = this.createAuth();
        auth.deleteUser();

        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.revokeAllSessionsUseCase.execute(new RevokeAllSessionsRequest(auth.getId()))
        );

        verifyNoInteractions(this.sessionRepository);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserIsInactive() {
        Authentication auth = this.createInactiveAuth();

        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.revokeAllSessionsUseCase.execute(new RevokeAllSessionsRequest(auth.getId()))
        );

        verifyNoInteractions(this.sessionRepository);
    }
}
