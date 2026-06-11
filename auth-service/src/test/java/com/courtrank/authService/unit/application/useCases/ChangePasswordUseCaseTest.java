package com.courtrank.authService.unit.application.useCases;

import com.courtrank.authService.application.dto.ChangePasswordRequest;
import com.courtrank.authService.application.ports.audit.AuditLogger;
import com.courtrank.authService.application.ports.security.PasswordHasher;
import com.courtrank.authService.application.useCases.ChangePasswordUseCase;
import com.courtrank.authService.domain.entity.Authentication;
import com.courtrank.authService.domain.enums.UserRole;
import com.courtrank.authService.domain.exceptions.InvalidCredentialsException;
import com.courtrank.authService.domain.exceptions.WeakPasswordException;
import com.courtrank.authService.domain.repository.AuthenticationRepository;
import com.courtrank.authService.domain.service.PasswordPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ChangePasswordUseCaseTest {

    @Mock
    AuthenticationRepository authenticationRepository;

    @Mock
    PasswordHasher passwordHasher;

    @Mock
    PasswordPolicy passwordPolicy;

    @Mock
    AuditLogger auditLogger;

    @InjectMocks
    ChangePasswordUseCase changePasswordUseCase;

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
    void execute_shouldChangePasswordWhenOldPasswordIsCorrect(){
        Authentication auth = this.createAuth();
        String oldPassword = "OldPass1!";
        String newPassword = "NewPass1!";
        String newPasswordHash = "new-hash";
        String currentPasswordHash = auth.getPasswordHash();

        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));

        when(this.passwordHasher.checkPassword(oldPassword, currentPasswordHash))
                .thenReturn(true);

        when(this.passwordHasher.hashPassword(newPassword))
                .thenReturn(newPasswordHash);

        this.changePasswordUseCase.execute(
                new ChangePasswordRequest(auth.getId(), oldPassword, newPassword)
        );

        assertEquals(newPasswordHash, auth.getPasswordHash());
        verify(this.passwordPolicy).validate(newPassword);
        verify(this.authenticationRepository).save(auth);
        verify(this.passwordHasher).checkPassword(oldPassword, currentPasswordHash);
        verify(this.passwordHasher).hashPassword(newPassword);
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        String oldPassword = "OldPass1!";
        String newPassword = "NewPass1!";

        when(this.authenticationRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.changePasswordUseCase.execute(
                        new ChangePasswordRequest(userId, oldPassword, newPassword)
                )
        );

        verify(this.authenticationRepository).findById(userId);
        verifyNoInteractions(this.passwordHasher);
        verifyNoInteractions(this.passwordPolicy);
        verify(this.authenticationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserIsDeleted() {
        Authentication auth = this.createAuth();
        auth.deleteUser();

        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.changePasswordUseCase.execute(
                        new ChangePasswordRequest(auth.getId(), "OldPass1!", "NewPass1!")
                )
        );

        verify(this.authenticationRepository).findById(auth.getId());
        verifyNoInteractions(this.passwordHasher);
        verifyNoInteractions(this.passwordPolicy);
        verify(this.authenticationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenUserIsInactive() {
        Authentication auth = this.createInactiveAuth();

        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.changePasswordUseCase.execute(
                        new ChangePasswordRequest(auth.getId(), "OldPass1!", "NewPass1!")
                )
        );

        verify(this.authenticationRepository).findById(auth.getId());
        verifyNoInteractions(this.passwordHasher);
        verifyNoInteractions(this.passwordPolicy);
        verify(this.authenticationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void execute_shouldThrowInvalidCredentialsWhenOldPasswordIsWrong() {
        Authentication auth = this.createAuth();
        String oldPassword = "WrongPass1!";
        String newPassword = "NewPass1!";
        String currentPasswordHash = auth.getPasswordHash();

        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));

        when(this.passwordHasher.checkPassword(oldPassword, currentPasswordHash))
                .thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> this.changePasswordUseCase.execute(
                        new ChangePasswordRequest(auth.getId(), oldPassword, newPassword)
                )
        );

        assertEquals(currentPasswordHash, auth.getPasswordHash());
        verify(this.passwordHasher).checkPassword(oldPassword, currentPasswordHash);
        verifyNoInteractions(this.passwordPolicy);
        verify(this.passwordHasher, never()).hashPassword(newPassword);
        verify(this.authenticationRepository, never()).save(auth);
    }

    @Test
    void execute_shouldThrowWeakPasswordWhenNewPasswordIsWeak() {
        Authentication auth = this.createAuth();
        String oldPassword = "OldPass1!";
        String weakPassword = "weak";
        String currentPasswordHash = auth.getPasswordHash();

        when(this.authenticationRepository.findById(auth.getId()))
                .thenReturn(Optional.of(auth));

        when(this.passwordHasher.checkPassword(oldPassword, currentPasswordHash))
                .thenReturn(true);

        doThrow(new WeakPasswordException("weak password"))
                .when(this.passwordPolicy)
                .validate(weakPassword);

        assertThrows(
                WeakPasswordException.class,
                () -> this.changePasswordUseCase.execute(
                        new ChangePasswordRequest(auth.getId(), oldPassword, weakPassword)
                )
        );

        assertEquals(currentPasswordHash, auth.getPasswordHash());
        verify(this.passwordHasher).checkPassword(oldPassword, currentPasswordHash);
        verify(this.passwordPolicy).validate(weakPassword);
        verify(this.passwordHasher, never()).hashPassword(weakPassword);
        verify(this.authenticationRepository, never()).save(auth);
    }
}
